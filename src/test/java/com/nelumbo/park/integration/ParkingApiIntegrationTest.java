package com.nelumbo.park.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.service.LoginAttemptTracker;
import com.nelumbo.park.support.RequiresPostgres;
import com.nelumbo.park.support.TestPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole application over real HTTP against PostgreSQL (schema from the
 * Flyway migrations), as a client uses it: Bearer token, no CSRF token.
 * Secrets are generated per run; nothing is read from .env.
 */
@RequiresPostgres
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.expiration-hours=1",
        "host.api.front=http://localhost:4200",
        "r2.bucket.access.key=test", "r2.bucket.secret.key=test", "r2.bucket.region=auto",
        "r2.account.id=test", "r2.bucket.name=test", "r2.bucket.path=test",
        "cron.determination=0 0 0 1 1 ?",
        "rabbitmq.url=amqp://127.0.0.1:1", "queue.name=test", "queue.final.name=-final",
        "app.subject=test", "type.message=test",
        "spring.mvc.contentnegotiation.media-types.pdf=application/pdf",
        "spring.mvc.contentnegotiation.media-types.xlsx=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "spring.jpa.show-sql=false"})
class ParkingApiIntegrationTest {

    private static final String ADMIN_PASSWORD = randomHex(12);
    private static final String CLIENT_IP = "127.0.0.1";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestPostgres.registerProperties(registry);
        registry.add("app.security.jwt.secret", () -> randomHex(32));
        registry.add("admin.password", () -> ADMIN_PASSWORD);
    }

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LoginAttemptTracker loginAttempts;
    @Autowired
    private PasswordHasherPort passwordHasher;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    private String adminToken;

    @BeforeEach
    void cleanAndLogin() throws Exception {
        jdbc.update("delete from vehicles");
        jdbc.update("delete from parking_lot");
        jdbc.update("delete from login_log");
        jdbc.update("delete from users_table where email <> 'admin@mail.com'");
        // The database may outlive one run: the seeded admin gets this run's password.
        jdbc.update("update users_table set password = ? where email = 'admin@mail.com'", passwordHasher.hash(ADMIN_PASSWORD));
        loginAttempts.loginSucceeded(CLIENT_IP);
        adminToken = login("admin@mail.com", ADMIN_PASSWORD);
    }

    @org.junit.jupiter.api.AfterAll
    static void leaveTheDatabaseEmpty() {
        TestPostgres.deleteAllRows();
    }

    // ---------------------------------------------------------------- login

    @Test
    void login_doesNotRevealWhetherTheEmailExists() throws Exception {
        Res wrongPassword = call("POST", "/auth/login", null, "{\"email\":\"admin@mail.com\",\"password\":\"wrong-pass\"}");
        Res unknownEmail = call("POST", "/auth/login", null, "{\"email\":\"nobody@mail.com\",\"password\":\"wrong-pass\"}");

        assertEquals(401, wrongPassword.status);
        assertEquals(401, unknownEmail.status);
        assertEquals(wrongPassword.body, unknownEmail.body);
        assertEquals("Credenciales incorrectas", json.readTree(unknownEmail.body).get("error").asText());
    }

    @Test
    void login_blocksTheIpAfterThreeFailures() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertEquals(401, call("POST", "/auth/login", null, "{\"email\":\"admin@mail.com\",\"password\":\"wrong-pass\"}").status);
        }
        Res third = call("POST", "/auth/login", null, "{\"email\":\"admin@mail.com\",\"password\":\"wrong-pass\"}");
        assertEquals(429, third.status, third.body);
        Res blocked = call("POST", "/auth/login", null,
                json.writeValueAsString(java.util.Map.of("email", "admin@mail.com", "password", ADMIN_PASSWORD)));
        assertEquals(429, blocked.status, "even the right password is refused while blocked");
        assertEquals(1, jdbc.queryForObject("select count(*) from login_log where description like 'Login Failed, IP Blocked:%'", Integer.class));
    }

    @Test
    void login_validatesTheBody() throws Exception {
        assertEquals(400, call("POST", "/auth/login", null, "{\"email\":\"not-an-email\",\"password\":\"123456\"}").status);
        assertEquals(400, call("POST", "/auth/login", null, "{\"email\":\"admin@mail.com\",\"password\":\"\"}").status);
    }

    @Test
    void anonymousRequests_areRejected() throws Exception {
        assertEquals(401, call("GET", "/parkings/", null, null).status);
        assertEquals(401, call("POST", "/vehicles/", null, "{}").status);
        assertEquals(401, call("GET", "/parkings/", "not-a-jwt", null).status);
    }

    // ---------------------------------------------------------------- users

    @Test
    void users_onlyAdminCreatesPartners_andDuplicatesConflict() throws Exception {
        Res created = createPartner("socio1");
        assertEquals(200, created.status, created.body);
        assertEquals("SOCIO", json.readTree(created.body).get("role").asText());

        assertEquals(409, createPartner("socio1").status);
        assertEquals(400, call("POST", "/users", adminToken,
                "{\"username\":\"bad name\",\"email\":\"x@mail.com\",\"password\":\"secret1\"}").status);

        String socio = login("socio1@mail.com", "secret1");
        assertEquals(403, call("POST", "/users", socio,
                "{\"username\":\"intruder\",\"email\":\"intruder@mail.com\",\"password\":\"secret1\"}").status);
        assertEquals(403, call("GET", "/users/", socio, null).status);
        assertEquals(200, call("GET", "/users/", adminToken, null).status);
    }

    // ---------------------------------------------------------------- parkings

    @Test
    void parkings_crudByAdmin_partnerSeesOnlyItsOwn() throws Exception {
        String ownerId = id(createPartner("socio1"));
        createPartner("socio2");
        String parkingId = createParking(ownerId, 5);
        String socio1 = login("socio1@mail.com", "secret1");
        String socio2 = login("socio2@mail.com", "secret1");

        assertEquals(403, call("POST", "/parkings/", socio1, parkingBody(ownerId, 5)).status);
        assertEquals(400, call("POST", "/parkings/", adminToken, parkingBody(ownerId, 0)).status);
        assertEquals(1, json.readTree(call("GET", "/parkings/", socio1, null).body).size());
        Res none = call("GET", "/parkings/", socio2, null);
        assertEquals(401, none.status, "legacy status for a partner without parkings");
        assertEquals("No tienes parkings asociados", json.readTree(none.body).get("error").asText());

        Res updated = call("PUT", "/parkings/" + parkingId, adminToken,
                "{\"name\":\"Parking Norte\",\"address\":\"Calle 100 # 10-20\",\"capacity\":7,\"costPerHour\":3000,\"idOwner\":\"" + ownerId + "\"}");
        assertEquals(200, updated.status, updated.body);
        assertEquals("Parking Norte", json.readTree(updated.body).get("name").asText());

        assertEquals(404, call("GET", "/parkings/does-not-exist", adminToken, null).status);
        assertEquals(200, call("DELETE", "/parkings/" + parkingId, adminToken, null).status);
        assertEquals(404, call("GET", "/parkings/" + parkingId, adminToken, null).status);
    }

    // ---------------------------------------------------------------- vehicles

    @Test
    void vehicleEntryAndExit_fullFlow() throws Exception {
        String ownerId = id(createPartner("socio1"));
        String parkingId = createParking(ownerId, 2);
        String socio = login("socio1@mail.com", "secret1");

        Res entry = call("POST", "/vehicles/", socio, vehicleBody("abc123", parkingId));
        assertEquals(200, entry.status, entry.body);
        assertEquals("ABC123", json.readTree(entry.body).get("plateNumber").asText());
        assertEquals("IN", json.readTree(entry.body).get("status").asText());

        Res again = call("POST", "/vehicles/", socio, vehicleBody("ABC123", parkingId));
        assertEquals(409, again.status);
        assertEquals("El vehiculo con placa ABC123 ya está registrado y actualmente en un parking",
                json.readTree(again.body).get("error").asText());

        assertEquals(200, call("POST", "/vehicles/", socio, vehicleBody("XYZ987", parkingId)).status);
        assertEquals(409, call("POST", "/vehicles/", socio, vehicleBody("FULL01", parkingId)).status, "capacity 2");
        assertEquals(404, call("POST", "/vehicles/", socio, vehicleBody("NOPE01", "missing-parking")).status);
        assertEquals(400, call("POST", "/vehicles/", socio, "{\"plateNumber\":\"\",\"modelVehicle\":\"x\",\"idParking\":\"x\"}").status);
        assertEquals(403, call("POST", "/vehicles/", adminToken, vehicleBody("ADM001", parkingId)).status, "SOCIO only");

        backdateEntry("ABC123", 90); // 1 h 30 min: two started hours at 2000/h
        Res exit = call("PUT", "/vehicles/", socio, exitBody("abc123", parkingId));
        assertEquals(200, exit.status, exit.body);
        JsonNode exitJson = json.readTree(exit.body);
        assertEquals("OUT", exitJson.get("status").asText());
        String totalCost = exitJson.get("totalCost").asText();
        assertTrue(totalCost.startsWith("$") && totalCost.endsWith("4.000"), exit.body);

        assertEquals(404, call("PUT", "/vehicles/", socio, exitBody("ABC123", parkingId)).status, "already out");
        assertEquals(200, call("POST", "/vehicles/", socio, vehicleBody("ABC123", parkingId)).status, "may enter again");
    }

    @Test
    void vehicles_partnersCannotTouchOtherPartnersVehicles() throws Exception {
        String owner1 = id(createPartner("socio1"));
        createPartner("socio2");
        String parkingId = createParking(owner1, 5);
        String socio1 = login("socio1@mail.com", "secret1");
        String socio2 = login("socio2@mail.com", "secret1");
        call("POST", "/vehicles/", socio1, vehicleBody("OWN001", parkingId));
        String vehicleId = jdbc.queryForObject("select id from vehicles where plate_number = 'OWN001'", String.class);

        assertEquals(403, call("POST", "/vehicles/", socio2, vehicleBody("OTH001", parkingId)).status);
        assertEquals(403, call("PUT", "/vehicles/", socio2, exitBody("OWN001", parkingId)).status);
        assertEquals(403, call("GET", "/vehicles/" + vehicleId, socio2, null).status);
        Res forbiddenDelete = call("DELETE", "/vehicles/" + vehicleId, socio2, null);
        assertEquals(403, forbiddenDelete.status);
        assertEquals(1, jdbc.queryForObject("select count(*) from vehicles", Integer.class));

        assertEquals(0, json.readTree(call("GET", "/vehicles/", socio2, null).body).size());
        assertEquals(1, json.readTree(call("GET", "/vehicles/", socio1, null).body).size());

        assertEquals(200, call("DELETE", "/vehicles/" + vehicleId, socio1, null).status);
        assertEquals(404, call("DELETE", "/vehicles/" + vehicleId, socio1, null).status);
    }

    @Test
    void samePlateCannotBeInTwoParkings() throws Exception {
        String owner = id(createPartner("socio1"));
        String a = createParking(owner, 5);
        String b = createParking(owner, 5);
        String socio = login("socio1@mail.com", "secret1");

        assertEquals(200, call("POST", "/vehicles/", socio, vehicleBody("TWO001", a)).status);
        assertEquals(409, call("POST", "/vehicles/", socio, vehicleBody("two001", b)).status);
    }

    // ---------------------------------------------------------------- indicators and rankings

    @Test
    void indicatorsAndRankings() throws Exception {
        String owner = id(createPartner("socio1"));
        String parkingId = createParking(owner, 5);
        String socio = login("socio1@mail.com", "secret1");
        call("POST", "/vehicles/", socio, vehicleBody("RET001", parkingId));
        backdateEntry("RET001", 120);
        call("PUT", "/vehicles/", socio, exitBody("RET001", parkingId));
        call("POST", "/vehicles/", socio, vehicleBody("RET001", parkingId));
        call("POST", "/vehicles/", socio, vehicleBody("NEW001", parkingId));

        Res firstTime = call("GET", "/indicators/", socio, null);
        assertEquals(200, firstTime.status, firstTime.body);
        JsonNode plates = json.readTree(firstTime.body);
        assertEquals(1, plates.size(), firstTime.body);
        assertEquals("NEW001", plates.get(0).get("plateNumber").asText());

        assertEquals(200, call("GET", "/rankings/", socio, null).status);
        assertEquals(200, call("GET", "/rankings/" + parkingId, socio, null).status);

        Res month = call("GET", "/parking-rankings/month", adminToken, null);
        assertEquals(200, month.status, month.body);
        JsonNode top = json.readTree(month.body).get("topParking");
        assertEquals(1, top.size(), month.body);
        assertEquals("4000.0f", top.get(0).get("totalCost").asText(), month.body);

        assertEquals(200, call("GET", "/parking-rankings/week", adminToken, null).status);
        assertEquals(200, call("GET", "/partners-rankings/week", adminToken, null).status);
        assertEquals(200, call("GET", "/partners-rankings/month", adminToken, null).status);
        assertEquals(403, call("GET", "/parking-rankings/month", socio, null).status);
        assertEquals(403, call("GET", "/partners-rankings/month", socio, null).status);
    }

    // ---------------------------------------------------------------- helpers

    private record Res(int status, String body) {
    }

    private Res call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://" + CLIENT_IP + ":" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(response.statusCode(), response.body());
    }

    private String login(String email, String password) throws Exception {
        Res res = call("POST", "/auth/login", null,
                json.writeValueAsString(java.util.Map.of("email", email, "password", password)));
        assertEquals(200, res.status, res.body);
        return json.readTree(res.body).get("accessToken").asText();
    }

    private Res createPartner(String username) throws Exception {
        return call("POST", "/users", adminToken, "{\"username\":\"" + username + "\",\"email\":\"" + username
                + "@mail.com\",\"password\":\"secret1\"}");
    }

    private String id(Res res) throws Exception {
        assertEquals(200, res.status, res.body);
        return json.readTree(res.body).get("id").asText();
    }

    private static String parkingBody(String ownerId, int capacity) {
        return "{\"name\":\"Parking Centro\",\"address\":\"Calle 10 # 20-30\",\"capacity\":" + capacity
                + ",\"costPerHour\":2000,\"idOwner\":\"" + ownerId + "\"}";
    }

    /** POST /parkings/ answers without the id (legacy format), so it is read back from the database. */
    private String createParking(String ownerId, int capacity) throws Exception {
        java.util.List<String> before = jdbc.queryForList("select id from parking_lot", String.class);
        Res res = call("POST", "/parkings/", adminToken, parkingBody(ownerId, capacity));
        assertEquals(200, res.status, res.body);
        java.util.List<String> created = new java.util.ArrayList<>(jdbc.queryForList("select id from parking_lot", String.class));
        created.removeAll(before);
        assertEquals(1, created.size());
        return created.getFirst();
    }

    private void backdateEntry(String plate, int minutes) {
        jdbc.update("update vehicles set entry_time = entry_time - make_interval(mins => ?) where plate_number = ? and status = 'IN'",
                minutes, plate);
    }

    private static String vehicleBody(String plate, String parkingId) {
        return "{\"plateNumber\":\"" + plate + "\",\"modelVehicle\":\"Mazda\",\"idParking\":\"" + parkingId + "\"}";
    }

    private static String exitBody(String plate, String parkingId) {
        return "{\"plateNumber\":\"" + plate + "\",\"modelVehicle\":\"Mazda\",\"idParking\":\"" + parkingId + "\"}";
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}

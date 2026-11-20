-- Baseline schema: exactly what Hibernate generated for the entities
-- (PostgreSQLDialect + PhysicalNamingStrategyStandardImpl) while the project
-- used ddl-auto=update. Existing databases already have it and are baselined
-- at version 1 (spring.flyway.baseline-on-migrate=true), so this script only
-- runs on empty databases.

create table "login_log" (
    date timestamp(6),
    id uuid not null,
    description varchar(255),
    email varchar(255),
    username varchar(255),
    primary key (id)
);

create table "parking_lot" (
    capacity integer not null,
    costPerHour float4 not null,
    address varchar(255) not null,
    id varchar(255) not null,
    id_owner varchar(255) not null,
    name varchar(255) not null,
    primary key (id)
);

create table "users_table" (
    email varchar(255) not null unique,
    id varchar(255) not null,
    password varchar(255) not null,
    role varchar(255) not null,
    username varchar(255) not null unique,
    primary key (id)
);

create table "vehicles" (
    cost_per_hour float4 not null,
    entry_time timestamp(6) not null,
    exit_time timestamp(6),
    id varchar(255) not null,
    id_admin varchar(255) not null,
    id_parking varchar(255) not null,
    model_vehicle varchar(255) not null,
    plate_number varchar(255) not null,
    status varchar(255) not null check (status in ('IN','OUT')),
    primary key (id)
);

alter table if exists "parking_lot"
    add constraint FKcxyqn7mkca1ht3vd62hnfveaq
    foreign key (id_owner)
    references "users_table";

alter table if exists "vehicles"
    add constraint FKegsc1iqs0aar7sksaw9msy87v
    foreign key (id_admin)
    references "users_table";

alter table if exists "vehicles"
    add constraint FKlykctt0r9e5yy87m08axww28k
    foreign key (id_parking)
    references "parking_lot";

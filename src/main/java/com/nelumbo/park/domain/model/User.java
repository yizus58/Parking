package com.nelumbo.park.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String username;
    /** Password hash, never the raw password. */
    private String password;
    private String email;
    private String role;

    public static User newPartner(String username, String email, String passwordHash) {
        return new User(null, username, passwordHash, email, Role.SOCIO.name());
    }

    public boolean isAdmin() {
        return Role.ADMIN.matches(role);
    }

    public boolean isPartner() {
        return Role.SOCIO.matches(role);
    }

    public boolean hasSameIdAs(User other) {
        return other != null && id != null && Objects.equals(id, other.getId());
    }
}

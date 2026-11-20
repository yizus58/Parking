package com.nelumbo.park.domain.model;

/**
 * Roles known by the application. Users store the role as text, so an
 * unknown value is possible and simply matches none of these.
 */
public enum Role {
    ADMIN,
    SOCIO;

    public boolean matches(String role) {
        return name().equals(role);
    }
}

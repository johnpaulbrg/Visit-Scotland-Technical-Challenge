package com.visitscotland.wishlist.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user who owns a wish list.
 * Immutable and identity-based.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
public final class User
{
    private final UUID id;
    private final String name;

    public User(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "User ID must not be null");
        this.name = Objects.requireNonNull(name, "User name must not be null").trim();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Equality based on UUID only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "'}";
    }
}

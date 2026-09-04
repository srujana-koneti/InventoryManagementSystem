package com.inventory.model;

import java.util.Objects;

/**
 * Represents a supplier in the inventory management system.
 * Encapsulates supplier details and basic validation rules.
 */
public class Supplier {
    private String id;
    private String name;
    private String contact;

    /**
     * Constructs a new Supplier with validation.
     *
     * @param id      Unique supplier ID (cannot be null or blank)
     * @param name    Supplier name (cannot be null or blank)
     * @param contact Supplier contact information, e.g. email or phone (cannot be null or blank)
     */
    public Supplier(String id, String name, String contact) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier Name cannot be null or empty.");
        }
        if (contact == null || contact.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier Contact cannot be null or empty.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.contact = contact.trim();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier ID cannot be null or empty.");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier Name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        if (contact == null || contact.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier Contact cannot be null or empty.");
        }
        this.contact = contact.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return Objects.equals(id, supplier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Supplier[ID=%s, Name='%s', Contact='%s']", id, name, contact);
    }
}

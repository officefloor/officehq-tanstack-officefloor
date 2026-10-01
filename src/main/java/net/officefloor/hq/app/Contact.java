package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A contact the user keeps for a client: a name, an email, a role, and the id of the client the
 * contact belongs to. Maps the {@code contacts} table (Flyway V11__contacts.sql); the id is
 * IDENTITY-generated on create. The role maps to the {@code contact_role} column.
 */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "contact_role", nullable = false)
    private String role;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    protected Contact() {
    }

    public Contact(String name, String email, String role, Long clientId) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getClientId() {
        return clientId;
    }

    /** Whether this contact is the client's main contact. */
    public boolean isPrimary() {
        return primary;
    }

    /** Mark (or unmark) this contact as the client's main contact. */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}

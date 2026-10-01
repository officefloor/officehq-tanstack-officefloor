package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A contact at a client: a name, an email and the person's role, plus the id of the owning client.
 * Persisted to the {@code contacts} table (Flyway V10). The id is database-generated (IDENTITY) on
 * create; the seed path inserts explicit ids directly via JdbcTemplate (see
 * {@link TestSupportController}). The {@code role} field maps to the {@code contact_role} column.
 */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id")
    private Long clientId;

    private String name;

    private String email;

    @Column(name = "contact_role")
    private String role;

    public Contact() {
    }

    public Contact(Long clientId, String name, String email, String role) {
        this.clientId = clientId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

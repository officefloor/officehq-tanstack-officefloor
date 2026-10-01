package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A project the user tracks for a client: a name and the id of the client it is for. Maps the
 * {@code projects} table (Flyway V3__projects.sql); the id is IDENTITY-generated on create.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    protected Project() {
    }

    public Project(String name, Long clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getClientId() {
        return clientId;
    }
}

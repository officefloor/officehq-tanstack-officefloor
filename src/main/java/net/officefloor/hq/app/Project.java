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

    // Whether the project has been archived (tucked away): an archived project is retained but drops
    // off the lists. New projects start not-archived (the column default, V16__project_archived.sql).
    @Column(nullable = false)
    private boolean archived;

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

    public boolean isArchived() {
        return archived;
    }

    /** Tuck this project away so it drops off the lists while the row is kept. */
    public void archive() {
        this.archived = true;
    }
}

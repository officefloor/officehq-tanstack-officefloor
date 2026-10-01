package net.officefloor.hq.app;

import java.math.BigDecimal;
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

    // Where the project sits in its lifecycle: ACTIVE, ON_HOLD or FINISHED. New projects start
    // ACTIVE (the column default, V22__project_status.sql); the allowed values are the names of
    // {@link ProjectStatus}.
    @Column(nullable = false)
    private String status = ProjectStatus.ACTIVE.name();

    // The planned spend set against the project, compared on the detail page with how much has been
    // invoiced. Money, so a {@link BigDecimal} (fixed scale, no float drift). Nullable: a budget is
    // optional, and a project without one has not had a budget set yet (V23__project_budget.sql).
    @Column
    private BigDecimal budget;

    protected Project() {
    }

    public Project(String name, Long clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public Project(String name, Long clientId, ProjectStatus status) {
        this.name = name;
        this.clientId = clientId;
        this.status = status.name();
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

    public String getStatus() {
        return status;
    }

    /** The planned spend set against this project, or {@code null} if no budget has been set. */
    public BigDecimal getBudget() {
        return budget;
    }

    /** Set (or change) the planned spend for this project. */
    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    /** Tuck this project away so it drops off the lists while the row is kept. */
    public void archive() {
        this.archived = true;
    }
}

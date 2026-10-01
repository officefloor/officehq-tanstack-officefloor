package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A checklist item on a project: a title, the id of the project it belongs to, and whether it is
 * done. Persisted to the {@code tasks} table (Flyway V12). The id is database-generated (IDENTITY)
 * on create; the seed path inserts explicit ids directly via JdbcTemplate (see
 * {@link TestSupportController}). A task is OPEN while {@code done} is false and DONE once ticked off.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    private String title;

    private boolean done;

    public Task() {
    }

    public Task(Long projectId, String title) {
        this.projectId = projectId;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}

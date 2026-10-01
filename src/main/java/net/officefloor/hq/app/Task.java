package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A to-do item the user tracks on a project: a title and the id of the project it is for. Maps the
 * {@code tasks} table (Flyway V14__tasks.sql); the id is IDENTITY-generated on create. The
 * {@code done} flag is the tick-off state — false while the task is OPEN, true once it is DONE.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean done;

    protected Task() {
    }

    public Task(Long projectId, String title) {
        this.projectId = projectId;
        this.title = title;
        this.done = false;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return done;
    }

    /** Tick this task off (or back on) — the OPEN &lt;-&gt; DONE transition the toggle performs. */
    public void toggleDone() {
        this.done = !this.done;
    }
}

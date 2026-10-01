package net.officefloor.hq.app;

/** The request body for ticking a task off (or back on): the id of the task to toggle. */
public class ToggleTaskForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

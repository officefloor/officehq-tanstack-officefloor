package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/tasks/toggle}: the id of the task to tick off (or un-tick). Bound
 * from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class ToggleTask {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

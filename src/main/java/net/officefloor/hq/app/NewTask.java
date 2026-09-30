package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/tasks}: the fields the user supplies when adding a task — the id
 * of the project it belongs to plus its title. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class NewTask {

    private Long projectId;

    private String title;

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
}

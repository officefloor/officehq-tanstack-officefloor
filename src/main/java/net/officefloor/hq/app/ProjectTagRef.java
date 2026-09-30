package net.officefloor.hq.app;

/**
 * Request body for the /api/project-tags add and remove routes: which tag to put on (or take off) of
 * which project. Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument
 * resolver.
 */
public class ProjectTagRef {

    private Long projectId;

    private Long tagId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}

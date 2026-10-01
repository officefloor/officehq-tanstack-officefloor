package net.officefloor.hq.app;

/** The request body for adding or removing a tag on a project: which project and which tag. */
public class ProjectTagForm {

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

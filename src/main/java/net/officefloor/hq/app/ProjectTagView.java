package net.officefloor.hq.app;

/**
 * What the /api/project-tags routes return: a project↔tag pairing plus the tag's NAME, so the UI can
 * render the chip label without a second lookup. Built by joining {@link ProjectTag} against
 * {@link Tag} in the logic classes.
 */
public class ProjectTagView {

    private final Long projectId;
    private final Long tagId;
    private final String name;

    public ProjectTagView(Long projectId, Long tagId, String name) {
        this.projectId = projectId;
        this.tagId = tagId;
        this.name = name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getTagId() {
        return tagId;
    }

    public String getName() {
        return name;
    }
}

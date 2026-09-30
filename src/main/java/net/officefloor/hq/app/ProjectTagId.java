package net.officefloor.hq.app;

import java.io.Serializable;
import java.util.Objects;

/**
 * The composite primary key of {@link ProjectTag}: the project and the tag it is paired with. Used
 * as the {@code @IdClass} so a (project, tag) pairing is addressable as one id.
 */
public class ProjectTagId implements Serializable {

    private Long projectId;

    private Long tagId;

    public ProjectTagId() {
    }

    public ProjectTagId(Long projectId, Long tagId) {
        this.projectId = projectId;
        this.tagId = tagId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectTagId other)) {
            return false;
        }
        return Objects.equals(projectId, other.projectId) && Objects.equals(tagId, other.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, tagId);
    }
}

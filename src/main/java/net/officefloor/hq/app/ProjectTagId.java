package net.officefloor.hq.app;

import java.io.Serializable;
import java.util.Objects;

/**
 * The composite key of {@link ProjectTag}: the (projectId, tagId) pair. A plain id-class so JPA can
 * address a join row by both columns — the field names mirror the {@code @Id} fields on ProjectTag.
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

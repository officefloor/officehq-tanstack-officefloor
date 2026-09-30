package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One project-to-label pairing: which {@link Tag} is attached to which {@link Project}. Persisted to
 * the {@code project_tags} join table (Flyway V13); the (project, tag) pair is its primary key
 * ({@link ProjectTagId}), so a tag cannot be attached to the same project twice.
 */
@Entity
@Table(name = "project_tags")
@IdClass(ProjectTagId.class)
public class ProjectTag {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    public ProjectTag() {
    }

    public ProjectTag(Long projectId, Long tagId) {
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
}

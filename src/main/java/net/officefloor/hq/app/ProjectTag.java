package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One label on one project: a row of the {@code project_tags} join (Flyway V17__project_tags.sql).
 * Its identity IS the (projectId, tagId) pair ({@link ProjectTagId}), so a tag sits on a project at
 * most once. Carries no data of its own beyond the two foreign keys.
 */
@Entity
@Table(name = "project_tags")
@IdClass(ProjectTagId.class)
public class ProjectTag {

    @Id
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Id
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    protected ProjectTag() {
    }

    public ProjectTag(Long projectId, Long tagId) {
        this.projectId = projectId;
        this.tagId = tagId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getTagId() {
        return tagId;
    }
}

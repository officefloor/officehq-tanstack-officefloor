package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The assignment of one {@link Tag} to one {@link Project} — a row in the {@code project_tags} join
 * table (Flyway V16). The surrogate id is database-generated (IDENTITY); the seed path inserts the
 * (project_id, tag_id) pair directly via JdbcTemplate (see {@link TestSupportController}). A project
 * is never tagged twice with the same tag (unique key in V16, guarded in the add logic).
 */
@Entity
@Table(name = "project_tags")
public class ProjectTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "tag_id")
    private Long tagId;

    public ProjectTag() {
    }

    public ProjectTag(Long projectId, Long tagId) {
        this.projectId = projectId;
        this.tagId = tagId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

package net.officefloor.hq.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A reusable label that can be put on projects to group them (e.g. "urgent", "retainer"). Persisted
 * to the {@code tags} table (Flyway V16). The id is database-generated (IDENTITY) on create; the seed
 * path inserts explicit ids directly via JdbcTemplate (see {@link TestSupportController}).
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

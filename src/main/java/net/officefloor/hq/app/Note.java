package net.officefloor.hq.app;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A free-text note written against a target (a project today, more over time): what it is about
 * ({@code targetType} + {@code targetId}), the note text, and the instant it was written. Persisted
 * to the {@code notes} table (Flyway V17). The id is database-generated (IDENTITY) on create; the
 * seed path inserts explicit ids directly via JdbcTemplate (see {@link TestSupportController}). Notes
 * are listed newest first, ordered by {@code at} descending.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "note_text")
    private String text;

    /** When the note was written. Serialized as an ISO instant, e.g. "2026-01-05T09:00:00Z". */
    private Instant at;

    public Note() {
    }

    public Note(String targetType, Long targetId, String text, Instant at) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.text = text;
        this.at = at;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}

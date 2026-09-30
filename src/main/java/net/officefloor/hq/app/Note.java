package net.officefloor.hq.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A free-text note written on a target (a {@link Project}, addressed by {@code targetType} +
 * {@code targetId}), carrying its text and the instant it was written (an ISO-8601 string so the
 * newest-first order is a plain descending sort). Persisted to the {@code notes} table (Flyway
 * V14). Jackson serialises the getters as the JSON the front-end reads.
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

    /** When the note was written, as an ISO-8601 instant string (e.g. 2026-01-02T09:00:00Z). */
    @Column(name = "noted_at")
    private String at;

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

    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }
}

package net.officefloor.hq.app;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A free-text note the user writes against something. Maps the {@code notes} table (Flyway
 * V18__notes.sql); the id is IDENTITY-generated on create. The target is generic — a type
 * ({@code "project"}) plus the target row's id — so the same notes surface can sit on any entity.
 * {@code createdAt} is when the note was written; the detail page lists notes newest-first by it.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Note() {
    }

    public Note(String targetType, Long targetId, String text) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

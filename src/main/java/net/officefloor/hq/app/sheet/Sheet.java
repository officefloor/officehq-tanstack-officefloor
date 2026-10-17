package net.officefloor.hq.app.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** A sheet: the unit everything else in the app is kept under. */
@Entity
@Table(name = "sheet")
public class Sheet {

    @Id
    private String id;

    @Column(name = "loaded_at", nullable = false)
    private Instant loadedAt;

    /** The sheet's "today": every date decision and default date uses it, never the real clock. */
    @Column(name = "today", nullable = false)
    private LocalDate today;

    protected Sheet() {
    }

    public Sheet(String id) {
        this.id = id;
        this.loadedAt = Instant.now();
        this.today = LocalDate.now();
    }

    public String getId() {
        return id;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public LocalDate getToday() {
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    public void markLoaded() {
        this.loadedAt = Instant.now();
    }
}

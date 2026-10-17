package net.officefloor.hq.app.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A sheet: the unit everything else in the app is kept under. */
@Entity
@Table(name = "sheet")
public class Sheet {

    @Id
    private String id;

    @Column(name = "loaded_at", nullable = false)
    private Instant loadedAt;

    protected Sheet() {
    }

    public Sheet(String id) {
        this.id = id;
        this.loadedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public void markLoaded() {
        this.loadedAt = Instant.now();
    }
}

package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** One of a person's snapshots: the day being left when the sheet's today moved on, and their due-total on it. */
@Embeddable
public class PersonSnapshot {

    @Column(name = "taken_on", nullable = false)
    private LocalDate takenOn;

    @Column(name = "due_total", nullable = false)
    private String dueTotal;

    protected PersonSnapshot() {
    }

    public PersonSnapshot(LocalDate takenOn, String dueTotal) {
        this.takenOn = takenOn;
        this.dueTotal = dueTotal;
    }

    public LocalDate getTakenOn() {
        return takenOn;
    }

    public String getDueTotal() {
        return dueTotal;
    }
}

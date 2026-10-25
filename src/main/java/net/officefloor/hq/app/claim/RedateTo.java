package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** The date a claim is to be re-dated to; once re-dated, the date it had before, for the audit. */
public class RedateTo {

    private final LocalDate date;
    private LocalDate oldDate;

    public RedateTo(LocalDate date) {
        this.date = date;
    }

    public LocalDate date() {
        return date;
    }

    public LocalDate oldDate() {
        return oldDate;
    }

    void redated(LocalDate oldDate) {
        this.oldDate = oldDate;
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated re-dating: the claim's new date and who re-dates it; `old` is the date it had, set once it is applied. */
public class ClaimRedate {
    private final LocalDate date;
    private final String user;
    private LocalDate old;

    public ClaimRedate(LocalDate date, String user) {
        this.date = date;
        this.user = user;
    }

    public LocalDate date() {
        return date;
    }

    public String user() {
        return user;
    }

    public LocalDate old() {
        return old;
    }

    public void redated(LocalDate old) {
        this.old = old;
    }
}

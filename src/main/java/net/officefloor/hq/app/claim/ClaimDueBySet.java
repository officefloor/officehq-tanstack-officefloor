package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated due-by setting: the claim's new due-by and who sets it; `old` is the due-by it had, set once it is applied. */
public class ClaimDueBySet {
    private final LocalDate dueBy;
    private final String user;
    private LocalDate old;

    public ClaimDueBySet(LocalDate dueBy, String user) {
        this.dueBy = dueBy;
        this.user = user;
    }

    public LocalDate dueBy() {
        return dueBy;
    }

    public String user() {
        return user;
    }

    public LocalDate old() {
        return old;
    }

    public void set(LocalDate old) {
        this.old = old;
    }
}

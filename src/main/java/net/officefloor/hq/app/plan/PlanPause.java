package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A pause of a plan: the sheet's today when it was paused, and when it was resumed (null while it stays paused). A
 *  plan date on or after {@code from} and before {@code until} is skipped. */
public record PlanPause(LocalDate from, LocalDate until) {

    public boolean skips(LocalDate date) {
        return !date.isBefore(from) && (until == null || date.isBefore(until));
    }
}

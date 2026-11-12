package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A pause of a plan: from the day it was paused to the day it was resumed ({@code resumed} null while still
 *  paused). A plan date on or after {@code paused} and before {@code resumed} is skipped. */
public record PlanPause(LocalDate paused, LocalDate resumed) {

    public boolean skips(LocalDate date) {
        return !date.isBefore(paused) && (resumed == null || date.isBefore(resumed));
    }
}

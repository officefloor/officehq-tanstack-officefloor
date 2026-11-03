package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A plan's pause: from the day it was paused (yyyy-mm-dd) to the day it was resumed (null while still paused).
 *  A plan date on or after from and before to is skipped. */
public record PlanPause(String from, String to) {

    public boolean skips(LocalDate on) {
        return !on.isBefore(LocalDate.parse(from)) && (to == null || on.isBefore(LocalDate.parse(to)));
    }
}

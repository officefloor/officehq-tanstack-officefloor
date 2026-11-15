package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A pause of a plan: the day it was paused and the day it was resumed (null while still paused). */
public record PlanPause(LocalDate pausedOn, LocalDate resumedOn) {

    /** Whether the plan date falls in the pause: on or after the day paused and before the day resumed. */
    public boolean skips(LocalDate date) {
        return !date.isBefore(pausedOn) && (resumedOn == null || date.isBefore(resumedOn));
    }
}

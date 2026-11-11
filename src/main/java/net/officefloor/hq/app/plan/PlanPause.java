package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A pause of a plan: from the day it was paused up to (not including) the day it was resumed, null while still paused. */
public record PlanPause(LocalDate pausedOn, LocalDate resumedOn) {

    /** Whether the plan date falls in this pause, and so is skipped. */
    public boolean skips(LocalDate date) {
        return !date.isBefore(pausedOn) && (resumedOn == null || date.isBefore(resumedOn));
    }
}

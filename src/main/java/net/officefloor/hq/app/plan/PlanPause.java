package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A plan's pause: from the day it was paused to the day it was resumed ({@code null} while still paused). */
public record PlanPause(LocalDate pausedOn, LocalDate resumedOn) {

    /** Whether the plan date falls in the pause: on or after its pause day and before its resume day. */
    public boolean skips(LocalDate date) {
        return !date.isBefore(pausedOn) && (resumedOn == null || date.isBefore(resumedOn));
    }
}

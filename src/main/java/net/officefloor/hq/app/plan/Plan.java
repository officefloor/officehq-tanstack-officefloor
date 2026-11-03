package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A person's plan: an amount (two decimals), a start date, every N days or monthly on a day of the month (1 to 31; the other null), an end date (yyyy-mm-dd),
 *  how many claims it has made so far, whether it is paused now, its pauses in the order made, and its amount changes, oldest first. */
public record Plan(long id, long personId, String amount, String start, Integer every, Integer day, String end, int generated,
        boolean paused, List<PlanPause> pauses, List<PlanChange> changes) {

    /** Whether a plan date falls in one of its pauses, so is skipped. */
    public boolean skips(LocalDate on) {
        return pauses.stream().anyMatch(p -> p.skips(on));
    }
}

package net.officefloor.hq.app.period;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Guards claims due by in a closed period (a claim belongs to the period containing its due-by):
 *  none can be added, edited, opened, settled or voided. */
@Service
public class ClosedPeriods {

    private final Periods periods;

    public ClosedPeriods(Periods periods) {
        this.periods = periods;
    }

    /** Refuse a change to a claim due by `dueBy` when a closed period contains it. */
    public void refuse(String sheetId, LocalDate dueBy) throws PeriodClosedException {
        Period closed = periods.closedOn(sheetId, dueBy);
        if (closed != null) throw new PeriodClosedException(closed, dueBy);
    }

    /** Whether the person is at the cap of the capped period containing `dueBy`: they already have as
     *  many OPEN or PENDING claims due by in it as it allows, so another cannot be opened. */
    public boolean atCap(String sheetId, long personId, LocalDate dueBy) {
        Period capped = periods.cappedOn(sheetId, dueBy);
        return capped != null && periods.openIn(capped, personId) >= capped.cap();
    }

    /** Whether a claim due by `dueBy` added now is a late entry: a soft-closed period contains it. */
    public boolean late(String sheetId, LocalDate dueBy) {
        return periods.softClosedOn(sheetId, dueBy) != null;
    }
}

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

    /** Whether a claim due by `dueBy` added now is a late entry: a soft-closed period contains it. */
    public boolean late(String sheetId, LocalDate dueBy) {
        return periods.softClosedOn(sheetId, dueBy) != null;
    }
}

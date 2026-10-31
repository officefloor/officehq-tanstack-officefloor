package net.officefloor.hq.app.period;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Guards claims dated in a closed period: none can be added, edited, opened, settled or voided. */
@Service
public class ClosedPeriods {

    private final Periods periods;

    public ClosedPeriods(Periods periods) {
        this.periods = periods;
    }

    /** Refuse a change to a claim dated `date` when a closed period contains it. */
    public void refuse(String sheetId, LocalDate date) throws PeriodClosedException {
        Period closed = periods.closedOn(sheetId, date);
        if (closed != null) throw new PeriodClosedException(closed, date);
    }
}

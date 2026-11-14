package net.officefloor.hq.app.rollover;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.GeneratedPeriods;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the rollover: the old fiscal year and how many periods were made. */
public class AuditRollover {
    public void audit(@Val Rollover rollover, @Val GeneratedPeriods generated, @Val Sheet sheet,
            @RequestParam("user") String user, Audit audit) {
        audit.record("ROLLOVER sheet=" + sheet.id() + " by=" + user + " fy=" + rollover.fy() + " periods="
                + generated.periods().size());
    }
}

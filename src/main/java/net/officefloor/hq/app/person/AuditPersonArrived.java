package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records, on the other sheet, the person just transferred there and the sheet they came from, then its rollup. */
public class AuditPersonArrived {
    public void audit(@Val Transfer transfer, @Val TransferTargetBefore before, @RequestParam("user") String user,
            RollupService rollups, Audit audit) {
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.to() + " by=" + user + " name="
                + transfer.person().name() + " from=" + transfer.person().sheetId());
        RollupLines.record(before.rollup(), rollups.compute(transfer.to()), transfer.to(), user, audit);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a transfer on both sheets: PERSON_TRANSFERRED sheet=<this> by=<user> name=<name> to=<other>, then
 *  PERSON_TRANSFERRED sheet=<other> by=<user> name=<name> from=<this>, each followed by how it moved that
 *  sheet's figures. */
public class AuditPersonTransferred {
    public void audit(@Val TransferRequest request, @Val Person person, @Val Transferred transferred,
            @Val Rollup before, Audit audit, RollupService rollups) {
        String from = person.getSheetId();
        String to = transferred.to();
        audit.record("PERSON_TRANSFERRED sheet=" + from + " by=" + request.user() + " name=" + person.getName()
                + " to=" + to);
        rollups.record(from, request.user(), before);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + request.user() + " name=" + person.getName()
                + " from=" + from);
        rollups.record(to, request.user(), transferred.toBefore());
    }
}

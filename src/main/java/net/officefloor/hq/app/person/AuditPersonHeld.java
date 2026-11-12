package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a hold: PERSON_HELD sheet=<id> by=<user> name=<name>, or PERSON_RELEASED when taken off it. */
public class AuditPersonHeld {
    public void audit(@Val HoldRequest request, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.getSheetId()
                + " by=" + request.user() + " name=" + person.getName());
        rollups.record(person.getSheetId(), request.user(), before);
    }
}

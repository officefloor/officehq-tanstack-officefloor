package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a hold: PERSON_HELD sheet=<id> by=<user> name=<name> from=<date>. */
public class AuditPersonHeld {
    public void audit(@Val HoldInput input, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record("PERSON_HELD sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " from=" + input.hold().from());
        rollups.record(person.getSheetId(), input.user(), before);
    }
}

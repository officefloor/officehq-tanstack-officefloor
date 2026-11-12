package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records an unretirement: PERSON_UNRETIRED sheet=<id> by=<user> name=<name> on=<date>, followed by how it moved the figures. */
public class AuditPersonUnretired {
    public void audit(@Val UnretireInput input, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " on=" + input.on());
        rollups.record(person.getSheetId(), input.user(), before);
    }
}

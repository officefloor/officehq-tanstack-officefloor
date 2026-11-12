package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a retirement: PERSON_RETIRED sheet=<id> by=<user> name=<name> from=<date>, followed by how it moved the figures. */
public class AuditPersonRetired {
    public void audit(@Val RetireInput input, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " from=" + person.getRetiredFrom());
        rollups.record(person.getSheetId(), input.user(), before);
    }
}

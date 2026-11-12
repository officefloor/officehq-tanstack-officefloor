package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored person add: PERSON_ADDED sheet=<id> by=<user> name=<name> grade=<grade>. */
public class AuditPersonAdded {
    public void audit(@Val PersonRequest request, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " grade=" + person.getGrade());
        rollups.record(person.getSheetId(), request.user(), before);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_EDITED to the audit file once the edit is stored. Then writes the
 * figures the change moved (see Rollup). */
public class RecordPersonEdited {
    public void record(@Val Person person, @Val PersonValues values, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " grade=" + person.grade());
        rollup.record(person.sheetId(), values.by(), before);
    }
}

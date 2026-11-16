package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_HELD to the audit file once the hold is stored. Then writes the figures the change moved (see Rollup). */
public class RecordPersonHold {
    public void record(@Val Person person, @Val Hold hold, @Val PersonHold added, @Val RollupBefore before, Rollup rollup,
            Audit audit) {
        audit.record("PERSON_HELD sheet=" + person.sheetId() + " by=" + hold.by() + " name=" + person.name() + " from="
                + added.from());
        rollup.record(person.sheetId(), hold.by(), before);
    }
}

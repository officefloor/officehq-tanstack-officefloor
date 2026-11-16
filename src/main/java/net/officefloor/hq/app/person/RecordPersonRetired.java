package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_RETIRED to the audit file once the retirement is stored. Then writes the figures the change moved
 *  (see Rollup): once their retirement date is reached, a retired person's OPEN and PENDING claims not bound by a
 *  statement leave every live total. */
public class RecordPersonRetired {
    public void record(@Val Person person, @Val Retirement retirement, @Val RollupBefore before, Rollup rollup,
            Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.sheetId() + " by=" + retirement.by() + " name=" + person.name()
                + " from=" + person.retiredFrom());
        rollup.record(person.sheetId(), retirement.by(), before);
    }
}

package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_UNRETIRED to the audit file once the unretirement is stored. Then writes the figures the change moved
 *  (see Rollup): the person's OPEN and PENDING claims count in the live totals again. */
public class RecordPersonUnretired {
    public void record(@Val Person person, @Val Unretirement unretirement, @Val RollupBefore before, Rollup rollup,
            Audit audit) {
        List<PersonRetirement> past = person.retirements();
        audit.record("PERSON_UNRETIRED sheet=" + person.sheetId() + " by=" + unretirement.by() + " name=" + person.name()
                + " on=" + past.get(past.size() - 1).to());
        rollup.record(person.sheetId(), unretirement.by(), before);
    }
}

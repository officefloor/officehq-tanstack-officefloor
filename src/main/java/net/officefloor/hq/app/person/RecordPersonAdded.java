package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_ADDED to the audit file once the person is stored, with terms= at the end when they were given, and
 * contact= after it when one was given. Then writes the
 * figures the change moved (see Rollup). */
public class RecordPersonAdded {
    public void record(@Val Person person, @Val PersonValues values, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " grade=" + person.grade() + (person.terms() == null ? "" : " terms=" + person.terms())
                + (person.contact() == null ? "" : " contact=" + person.contact()));
        rollup.record(person.sheetId(), values.by(), before);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_EDITED to the audit file once the edit is stored, with terms= at the end when the edit changed the
 * person's own terms (empty when it cleared them). Then writes the
 * figures the change moved (see Rollup). */
public class RecordPersonEdited {
    public void record(@Val Person person, @Val PersonValues values, @Val TermsBefore termsBefore, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " grade=" + person.grade() + (java.util.Objects.equals(termsBefore.days(), person.terms()) ? ""
                        : " terms=" + (person.terms() == null ? "" : person.terms())));
        rollup.record(person.sheetId(), values.by(), before);
    }
}

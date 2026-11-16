package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_EDITED to the audit file once the edit is stored, with terms= at the end when the edit changed the
 * person's own terms (empty when it cleared them), and contact= after it
 * when the edit changed the person's contact (empty when it cleared it). Then writes the
 * figures the change moved (see Rollup). */
public class RecordPersonEdited {
    public void record(@Val Person person, @Val PersonValues values, @Val TermsBefore termsBefore,
            @Val ContactBefore contactBefore, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " grade=" + person.grade() + (java.util.Objects.equals(termsBefore.days(), person.terms()) ? ""
                        : " terms=" + (person.terms() == null ? "" : person.terms()))
                + (java.util.Objects.equals(contactBefore.contact(), person.contact()) ? ""
                        : " contact=" + (person.contact() == null ? "" : person.contact())));
        rollup.record(person.sheetId(), values.by(), before);
    }
}

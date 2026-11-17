package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_FLAGGED to the audit file once the flag is stored. */
public class RecordPersonFlagged {
    public void record(@Val Person person, @Val Flag flag, @Val PersonFlag stored, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.sheetId() + " by=" + flag.by() + " name=" + person.name()
                + " reason=" + stored.reason() + " until=" + stored.until());
    }
}

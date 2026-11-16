package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_RETIRED to the audit file once the retirement is stored. Retiring moves no figure: a retired
 *  person's claims keep counting in every total. */
public class RecordPersonRetired {
    public void record(@Val Person person, @Val Retirement retirement, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.sheetId() + " by=" + retirement.by() + " name=" + person.name()
                + " from=" + person.retiredFrom());
    }
}

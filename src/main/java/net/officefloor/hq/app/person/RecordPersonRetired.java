package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the retirement is saved, so a refused one writes no audit line. */
public class RecordPersonRetired {
    public void record(@Val Retirement retirement, @Val Person person, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + retirement.user()
                + " name=" + person.getName() + " from=" + person.getRetiredFrom());
    }
}

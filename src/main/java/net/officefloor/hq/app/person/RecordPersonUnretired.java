package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the unretirement is saved, so a refused one writes no audit line. */
public class RecordPersonUnretired {
    public void record(@Val Unretirement unretirement, @Val Person person, Audit audit) {
        audit.record("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + unretirement.user()
                + " name=" + person.getName() + " on=" + unretirement.on());
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored retire in the audit file. */
public class AuditPersonRetired {
    public void audit(@Val RetireInput input, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " name=" + saved.name() + " from=" + saved.retiredFrom());
    }
}

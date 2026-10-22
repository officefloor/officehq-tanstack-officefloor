package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored hold in the audit file, with the date it runs from. */
public class AuditPersonHeld {
    public void audit(@Val HoldInput input, @Val Person loaded, @Val Holds.Hold hold, Audit audit) {
        audit.record("PERSON_HELD sheet=" + loaded.sheetId() + " by=" + input.user() + " name=" + loaded.name()
                + " from=" + hold.from());
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored hold, or release, in the audit file. */
public class AuditPersonHeld {
    public void audit(@Val HoldRequest request, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        audit.record((saved.held() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + loaded.sheetId()
                + " by=" + request.user() + " name=" + saved.name());
    }
}

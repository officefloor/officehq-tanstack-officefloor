package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored edit in the audit file. */
public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + loaded.sheetId() + " by=" + request.user()
                + " name=" + saved.name() + " grade=" + saved.grade()
                + (request.terms() != null && !request.terms().isEmpty() ? " terms=" + request.terms() : ""));
    }
}

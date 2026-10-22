package net.officefloor.hq.app.person;

import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored edit in the audit file; the person's own terms only when the edit changes them. */
public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + loaded.sheetId() + " by=" + request.user()
                + " name=" + saved.name() + " grade=" + saved.grade()
                + (request.terms() != null && !request.terms().isEmpty()
                        && !Objects.equals(Integer.valueOf(request.terms()), loaded.terms())
                        ? " terms=" + request.terms() : ""));
    }
}

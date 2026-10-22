package net.officefloor.hq.app.person;

import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored edit in the audit file; the person's own terms whenever the edit gives them, or when it clears
 *  terms the person had (blank: cleared). */
public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        Integer terms = request.terms() == null || request.terms().isEmpty() ? null : Integer.valueOf(request.terms());
        audit.record("PERSON_EDITED sheet=" + loaded.sheetId() + " by=" + request.user()
                + " name=" + saved.name() + " grade=" + saved.grade()
                + (request.terms() != null && (terms != null || !Objects.equals(terms, loaded.terms()))
                        ? " terms=" + request.terms() : ""));
    }
}

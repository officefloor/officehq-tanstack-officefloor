package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored add in the audit file. */
public class AuditPersonAdded {
    public void audit(@Val PersonRequest request, @Val Person person, Audit audit) {
        record(request, person, audit);
    }

    static void record(PersonRequest request, Person person, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.sheetId() + " by=" + request.user()
                + " name=" + person.name() + " grade=" + person.grade()
                + (request.terms() != null && !request.terms().isEmpty() ? " terms=" + request.terms() : "")
                + (request.contact() != null && !request.contact().isEmpty() ? " contact=" + request.contact() : ""));
    }
}

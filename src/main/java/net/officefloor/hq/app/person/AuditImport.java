package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_ADDED or PERSON_EDITED for each imported person, in paste order, once all are saved. */
public class AuditImport {
    public void audit(@Val ImportedPeople imported, @HttpHeaderParameter("X-User") String user, Audit audit) {
        imported.people().forEach((person) -> audit.record(
                PersonAudit.line(imported.isUpdated(person) ? "PERSON_EDITED" : "PERSON_ADDED", person, user)));
    }
}

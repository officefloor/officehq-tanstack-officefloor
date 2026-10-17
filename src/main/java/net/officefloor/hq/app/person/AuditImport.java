package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERSON_ADDED for each imported person, in order, once all are saved. */
public class AuditImport {
    public void audit(@Val ImportedPeople imported, @HttpHeaderParameter("X-User") String user, Audit audit) {
        imported.people().forEach((person) -> audit.record(PersonAudit.line("PERSON_ADDED", person, user)));
    }
}

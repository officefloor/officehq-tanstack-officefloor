package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_OPENED for each claim a bulk open opened, in the order they were numbered. */
public class AuditBulkOpened {
    public void audit(@Val BulkOpened bulk, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        bulk.opened().forEach((claim) -> audit.record(ClaimAudit.line("CLAIM_OPENED", claim, user, people)));
    }
}

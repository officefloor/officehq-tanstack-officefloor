package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditReviewAssigned {
    public void audit(@Val Claim loaded, @Val ClaimResponse assigned, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        audit.record(Audit.line("REVIEW_ASSIGNED", loaded.sheetId(), Audit.user(connection),
                "person=" + (person == null ? "" : person.name()), "amount=" + assigned.amount(),
                "date=" + assigned.date(), "user=" + assigned.reviewUser()));
    }
}

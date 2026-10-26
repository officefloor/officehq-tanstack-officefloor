package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimEdited {
    // The loaded claim names the sheet and person; the edited one (as the screen sees it) the values.
    public void audit(@Val Claim loaded, @Val ClaimResponse edited, People people,
            ServerHttpConnection connection, Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        audit.record(Audit.line("CLAIM_EDITED", loaded.sheetId(), Audit.user(connection),
                "person=" + (person == null ? "" : person.name()), "amount=" + edited.amount(),
                "date=" + edited.date()));
    }
}

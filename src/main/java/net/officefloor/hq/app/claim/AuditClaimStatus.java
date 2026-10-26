package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimStatus {
    public void audit(@Val Claim loaded, @Val ClaimResponse changed, People people,
            ServerHttpConnection connection, Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        audit.record(Audit.line(ClaimStatus.valueOf(changed.status()).event(), loaded.sheetId(),
                Audit.user(connection), "person=" + (person == null ? "" : person.name()),
                "amount=" + changed.amount(), "date=" + changed.date()));
    }
}

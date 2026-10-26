package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimSplit {
    // CLAIM_SPLIT names the original at the amount it keeps, ending with the new claim's number; then the new claim's
    // CLAIM_OPENED.
    public void audit(@Val Split split, People people, ServerHttpConnection connection, Audit audit) {
        Claim original = split.original();
        Claim created = split.created();
        Person person = people.find(original.sheetId(), original.personId());
        String name = "person=" + (person == null ? "" : person.name());
        String user = Audit.user(connection);
        audit.record(Audit.line("CLAIM_SPLIT", original.sheetId(), user, name,
                "amount=" + original.amount().toPlainString(), "date=" + original.date(), "number=" + original.number(),
                "into=" + created.number()));
        audit.record(Audit.line(ClaimStatus.OPEN.event(), created.sheetId(), user, name,
                "amount=" + created.amount().toPlainString(), "date=" + created.date(), "number=" + created.number()));
    }
}

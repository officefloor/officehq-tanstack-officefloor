package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person,
            ServerHttpConnection connection, Audit audit) {
        ClaimResponse shown = ClaimResponse.from(claim);
        List<String> fields = new ArrayList<>(List.of("person=" + person.name(), "amount=" + shown.amount(),
                "date=" + shown.date()));
        if (claim.ref() != null) {
            fields.add("ref=" + claim.ref());
        }
        if (!claim.tags().isEmpty()) {
            fields.add("tags=" + String.join(",", claim.tags()));
        }
        audit.record(Audit.line("CLAIM_ADDED", claim.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
        if (claim.lateEntry()) {
            audit.record(Audit.line("LATE_ENTRY", claim.sheetId(), Audit.user(connection), "person=" + person.name(),
                    "amount=" + shown.amount(), "date=" + shown.date()));
        }
    }
}

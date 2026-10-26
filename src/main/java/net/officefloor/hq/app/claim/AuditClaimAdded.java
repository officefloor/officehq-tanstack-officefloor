package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person,
            ServerHttpConnection connection, Audit audit) {
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record(Audit.line("CLAIM_ADDED", claim.sheetId(), Audit.user(connection), "person=" + person.name(),
                "amount=" + shown.amount(), "date=" + shown.date()));
        if (claim.lateEntry()) {
            audit.record(Audit.line("LATE_ENTRY", claim.sheetId(), Audit.user(connection), "person=" + person.name(),
                    "amount=" + shown.amount(), "date=" + shown.date()));
        }
    }
}

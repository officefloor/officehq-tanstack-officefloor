package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimCommented {
    public void audit(@Val Claim claim, @Val ClaimComment comment, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + claim.amount().toPlainString(), "date=" + claim.date()));
        if (claim.number() != null) {
            fields.add("number=" + claim.number());
        }
        fields.add("text=" + comment.text());
        audit.record(Audit.line("COMMENT_ADDED", claim.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}

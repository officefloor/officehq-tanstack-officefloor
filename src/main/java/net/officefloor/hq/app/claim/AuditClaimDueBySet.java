package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimDueBySet {
    public void audit(@Val Claim loaded, @Val ClaimResponse set, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + set.amount(), "date=" + set.date()));
        if (set.number() != null) {
            fields.add("number=" + set.number());
        }
        fields.add("old=" + loaded.dueBy());
        fields.add("new=" + set.dueBy());
        audit.record(Audit.line("CLAIM_DUE_BY_SET", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}

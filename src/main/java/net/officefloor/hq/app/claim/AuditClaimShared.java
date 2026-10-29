package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimShared {
    public void audit(@Val Claim loaded, @Val Shared shared, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + loaded.amount().toPlainString(), "date=" + loaded.date()));
        if (loaded.number() != null) {
            fields.add("number=" + loaded.number());
        }
        fields.add("with=" + shared.with());
        fields.add("share=" + shared.share().share().toPlainString());
        audit.record(Audit.line("CLAIM_SHARED", loaded.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}

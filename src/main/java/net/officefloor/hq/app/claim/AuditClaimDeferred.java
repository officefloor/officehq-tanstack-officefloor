package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimDeferred {
    public void audit(@Val Claim loaded, @Val ClaimResponse deferred, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + deferred.amount(), "date=" + deferred.date()));
        if (deferred.number() != null) {
            fields.add("number=" + deferred.number());
        }
        fields.add("until=" + deferred.deferredUntil());
        audit.record(Audit.line("CLAIM_DEFERRED", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}

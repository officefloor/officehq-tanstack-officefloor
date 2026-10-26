package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimResettled {
    public void audit(@Val Claim loaded, @Val Resettled resettled, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + loaded.amount().toPlainString(), "date=" + loaded.date()));
        if (loaded.number() != null) {
            fields.add("number=" + loaded.number());
        }
        fields.add("old=" + figure(resettled.old()));
        fields.add("new=" + figure(resettled.now()));
        audit.record(Audit.line("CLAIM_RESETTLED", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }

    private static String figure(BigDecimal due) {
        return due == null ? "n/a" : due.toPlainString();
    }
}

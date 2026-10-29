package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimSplit {
    // CLAIM_SPLIT names the original at the amount it keeps, ending with the new claim's number (empty: none yet); then the new claim's
    // CLAIM_OPENED.
    public void audit(@Val Split split, People people, ServerHttpConnection connection, Audit audit) {
        Claim original = split.original();
        Claim created = split.created();
        Person person = people.find(original.sheetId(), original.personId());
        String name = "person=" + (person == null ? "" : person.name());
        String user = Audit.user(connection);
        // An unnumbered claim's lines carry no number, and an unnumbered new claim leaves into= empty.
        java.util.List<String> fields = new java.util.ArrayList<>(java.util.List.of(name,
                "amount=" + original.amount().toPlainString(), "date=" + original.date()));
        if (original.number() != null) {
            fields.add("number=" + original.number());
        }
        fields.add("into=" + (created.number() == null ? "" : created.number()));
        audit.record(Audit.line("CLAIM_SPLIT", original.sheetId(), user, fields.toArray(String[]::new)));
        java.util.List<String> opened = new java.util.ArrayList<>(java.util.List.of(name,
                "amount=" + created.amount().toPlainString(), "date=" + created.date()));
        if (created.number() != null) {
            opened.add("number=" + created.number());
        }
        audit.record(Audit.line(ClaimStatus.OPEN.event(), created.sheetId(), user, opened.toArray(String[]::new)));
    }
}

package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditClaimProofAdded {
    public void audit(@Val Claim loaded, @Val ClaimResponse proved, @Val ProofRequest proof, People people, ServerHttpConnection connection,
            Audit audit) {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        List<String> fields = new ArrayList<>(List.of("person=" + (person == null ? "" : person.name()),
                "amount=" + proved.amount(), "date=" + proved.date()));
        if (proved.number() != null) {
            fields.add("number=" + proved.number());
        }
        fields.add("proofs=" + proved.proofs());
        if (!proof.chosen().isEmpty()) {
            fields.add("kind=" + proof.chosen());
        }
        audit.record(Audit.line("CLAIM_PROOF_ADDED", loaded.sheetId(), Audit.user(connection),
                fields.toArray(String[]::new)));
    }
}

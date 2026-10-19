package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the added claim once every rule has passed and it is stored. */
public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person,
            @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("CLAIM_ADDED sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + claim.getAmount().toPlainString() + " date=" + claim.getDate());
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim just added; runs only once the add has passed every rule. */
public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + user + " person=" + person.name()
                + " amount=" + claim.amount().setScale(2).toPlainString() + " date=" + claim.date());
    }
}

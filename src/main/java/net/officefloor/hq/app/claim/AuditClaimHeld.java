package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim just held (CLAIM_HELD) or released (CLAIM_RELEASED), with its person's name. */
public class AuditClaimHeld {
    public void audit(@Val Claim claim, PersonRepository people, @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record((claim.held() ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + claim.sheetId() + " by=" + user
                + " person=" + (person == null ? "" : person.name()) + " amount="
                + claim.amount().setScale(2).toPlainString() + " date=" + claim.date() + Claim.auditNumber(claim.number()));
    }
}

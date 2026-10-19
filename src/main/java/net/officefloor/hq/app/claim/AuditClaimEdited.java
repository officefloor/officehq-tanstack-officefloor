package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the edited claim once every rule has passed and it is saved. */
public class AuditClaimEdited {
    public void audit(@Val Claim claim, @RequestParam(value = "user", required = false) String user,
            PersonRepository people, Audit audit) {
        Person person = people.findById(claim.getSheetId(), claim.getPersonId());
        audit.record("CLAIM_EDITED sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + claim.getAmount().toPlainString() + " date=" + claim.getDate()
                + new ClaimNumber(claim.getNumber()).auditField());
    }
}

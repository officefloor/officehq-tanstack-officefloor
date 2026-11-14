package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim whose due-by was just set, with its person's name, then its old and new due-by. */
public class AuditClaimDueBySet {
    public void audit(@Val Claim claim, @Val DueBySetting setting, PersonRepository people,
            @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_DUE_BY_SET sheet=" + claim.sheetId() + " by=" + user + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(claim.number()) + " old=" + setting.from() + " new="
                + setting.to());
    }
}

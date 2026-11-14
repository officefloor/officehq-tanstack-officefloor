package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim just shared, with its person's name, the name of the person given the share and the share. */
public class AuditClaimShared {
    public void audit(@Val Claim claim, @Val ClaimShare share, PersonRepository people,
            @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        Person with = people.find(claim.sheetId(), share.personId());
        audit.record("CLAIM_SHARED sheet=" + claim.sheetId() + " by=" + user + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(claim.number()) + " with="
                + (with == null ? "" : with.name()) + " share=" + share.share().toPlainString());
    }
}

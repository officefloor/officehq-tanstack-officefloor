package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the split, naming the original with its remaining amount and the new claim's number, then the new claim's opening. */
public class AuditClaimSplit {
    public void audit(@Val ClaimSplit split, PersonRepository people, @RequestParam("user") String user, Audit audit) {
        Claim original = split.original();
        Claim created = split.created();
        Person person = people.find(original.sheetId(), original.personId());
        String name = person == null ? "" : person.name();
        audit.record("CLAIM_SPLIT sheet=" + original.sheetId() + " by=" + user + " person=" + name + " amount="
                + original.amount().setScale(2).toPlainString() + " date=" + original.date()
                + Claim.auditNumber(original.number()) + " into=" + created.number());
        audit.record(ClaimAction.OPEN.event() + " sheet=" + created.sheetId() + " by=" + user + " person=" + name
                + " amount=" + created.amount().setScale(2).toPlainString() + " date=" + created.date()
                + Claim.auditNumber(created.number()));
    }
}

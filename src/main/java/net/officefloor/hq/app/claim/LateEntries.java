package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import org.springframework.stereotype.Component;

/**
 * Marks a claim just added a late entry when the period containing its date is soft-closed, recorded as LATE_ENTRY
 * (after its CLAIM_ADDED); nothing otherwise. Used by the claim form, the quick-add and the paste.
 */
@Component
public class LateEntries {

    private final PeriodRepository periods;
    private final ClaimRepository claims;
    private final Audit audit;

    public LateEntries(PeriodRepository periods, ClaimRepository claims, Audit audit) {
        this.periods = periods;
        this.claims = claims;
        this.audit = audit;
    }

    public void mark(Claim claim, Person person, String user) {
        if (periods.softClosedOn(claim.sheetId(), claim.date()) == null) {
            return;
        }
        claims.markLateEntry(claim);
        audit.record("LATE_ENTRY sheet=" + claim.sheetId() + " by=" + user + " person=" + person.name()
                + " amount=" + claim.amount().setScale(2).toPlainString() + " date=" + claim.date());
    }
}

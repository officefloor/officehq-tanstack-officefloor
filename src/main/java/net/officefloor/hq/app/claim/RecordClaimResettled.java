package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused re-settle writes no log or audit line. */
public class RecordClaimResettled {
    public void record(@Val ClaimResettle change, @Val Claim claim, People people, ClaimLog log, Audit audit) {
        log.write(claim, change.user());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record("CLAIM_RESETTLED sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + " old=" + figure(change.old()) + " new=" + figure(claim.getDue()));
    }

    private static String figure(BigDecimal due) {
        return due != null ? due.toPlainString() : "n/a";
    }
}

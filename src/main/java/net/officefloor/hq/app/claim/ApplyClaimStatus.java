package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyClaimStatus {
    // Stored only if the claim still has the status it was loaded with, so two users cannot both move it.
    public void apply(@PathVariable("status") String status, @Val Claim claim, Claims claims,
            People people, Rates rates,
            Out<ClaimResponse> responseOut) throws ClaimStatusRefusedException {
        ClaimStatus next;
        try {
            next = ClaimStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClaimStatusRefusedException("Not a claim status: " + status);
        }
        Claim changed = claim.status().allows(next) ? claims.changeStatus(claim, next, due(claim, people, rates)) : null;
        if (changed == null) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        responseOut.set(ClaimResponse.from(changed));
    }

    /** The claim's due now: its amount times its person's grade's rate on the claim's date, half to even; null when none applies. */
    private static BigDecimal due(Claim claim, People people, Rates rates) {
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person == null) {
            return null;
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null : claim.amount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}

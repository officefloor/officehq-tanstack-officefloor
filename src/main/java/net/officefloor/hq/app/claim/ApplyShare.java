package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Shares a claim with another person on the sheet: not its own person, not one already holding a share, the shares
 *  totalling at most 100.00; a SETTLED, VOID or DELETED claim cannot be shared. */
public class ApplyShare {
    public void apply(@Val ShareRequest request, @Val Claim claim, Claims claims, People people, Out<Person> holderOut)
            throws ClaimStatusRefusedException {
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new ClaimStatusRefusedException("A " + status + " claim cannot be shared");
        }
        Person holder = people.find(claim.getSheetId(), Long.parseLong(request.personId()));
        if (holder == null) {
            throw new ClaimStatusRefusedException("No such person on the sheet");
        }
        if (holder.getId() == claim.getPersonId()) {
            throw new ClaimStatusRefusedException("A claim cannot be shared with its own person");
        }
        BigDecimal total = new BigDecimal(request.share());
        for (ClaimShare held : claims.shares(claim)) {
            if (held.personId() == holder.getId()) {
                throw new ClaimStatusRefusedException(holder.getName() + " already holds a share of the claim");
            }
            total = total.add(held.share());
        }
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ClaimStatusRefusedException("The claim's shares would total more than 100.00");
        }
        holderOut.set(holder);
    }
}

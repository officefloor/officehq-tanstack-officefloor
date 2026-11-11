package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Shares the claim with another person on the sheet. A SETTLED, VOID or DELETED claim is refused; so
 * is sharing it with its own person, with a person already holding a share, or past 100.00 in all.
 * A refusal writes nothing.
 */
public class ShareClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val Sharing sharing, PersonRepository people,
            ClaimRepository claims, Out<Person> holderOut) throws InvalidClaimStatusException, InvalidClaimException {
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot be shared");
        }
        Person holder = people.find(sheet.id(), sharing.personId());
        if (holder == null) throw new InvalidClaimException("No such person to share the claim with");
        if (holder.getId() == claim.getPersonId()) {
            throw new InvalidClaimException("A claim cannot be shared with its own person");
        }
        List<ClaimShare> held = claims.sharesOf(claim);
        if (held.stream().anyMatch((s) -> s.personId() == holder.getId())) {
            throw new InvalidClaimException(holder.getName() + " already holds a share of the claim");
        }
        if (ClaimShare.total(held).add(sharing.share()).compareTo(new BigDecimal("100.00")) > 0) {
            throw new InvalidClaimException("A claim's shares cannot total more than 100.00");
        }
        claims.addShare(claim, new ClaimShare(holder.getId(), sharing.share()));
        holderOut.set(holder);
    }
}

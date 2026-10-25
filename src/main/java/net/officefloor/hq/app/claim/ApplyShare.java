package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Shares the claim with another person on the sheet. A SETTLED, VOID or DELETED claim cannot be shared; nor can it be
 * shared with its own person, with a person already holding a share of it, or so its shares total more than 100.00.
 */
public class ApplyShare {
    public void apply(@Val ShareRequest request, @Val Claim claim, PersonRepository people, ClaimRepository repository,
            Out<Person> holder) throws ClaimStatusException, InvalidClaimException {
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new ClaimStatusException("A " + status + " claim cannot be shared.");
        }
        Long personId = Long.valueOf(request.personId());
        Person person = people.findById(personId).filter(p -> p.getSheetId().equals(claim.getSheetId()))
                .orElseThrow(() -> new InvalidClaimException("No such person on the sheet."));
        if (personId.equals(claim.getPersonId())) {
            throw new InvalidClaimException("A claim cannot be shared with its own person.");
        }
        List<ClaimShares.Share> shares = ClaimShares.of(claim);
        if (shares.stream().anyMatch(s -> s.personId().equals(personId))) {
            throw new InvalidClaimException(person.getName() + " already holds a share of this claim.");
        }
        BigDecimal share = new BigDecimal(request.share());
        if (ClaimShares.total(shares).add(share).compareTo(new BigDecimal("100.00")) > 0) {
            throw new InvalidClaimException("A claim's shares cannot total more than 100.00.");
        }
        claim.setShares(ClaimShares.adding(claim, personId, share));
        repository.saveAndFlush(claim);
        holder.set(person);
    }
}

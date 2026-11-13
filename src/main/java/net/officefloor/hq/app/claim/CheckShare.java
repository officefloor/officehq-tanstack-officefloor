package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Refuses sharing a SETTLED, VOID or DELETED claim, or sharing it with a person not on the sheet, with its own
 *  person, with a person already holding a share of it, or so its shares total more than 100.00; publishes the
 *  person it is shared with. */
public class CheckShare {
    private static final BigDecimal WHOLE = new BigDecimal("100.00");

    public void check(@Val ShareInput input, @Val Claim claim, PersonStore people, Out<Person> holder)
            throws InvalidSheetException {
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new InvalidSheetException("A " + status + " claim cannot be shared");
        }
        Person person = people.find(claim.getSheetId(), input.personId());
        if (person == null) {
            throw new InvalidSheetException("No such person: " + input.personId());
        }
        if (person.getId() == claim.getPersonId()) {
            throw new InvalidSheetException("A claim cannot be shared with its own person");
        }
        List<Shares.Share> shares = Shares.list(claim.getShares());
        if (shares.stream().anyMatch(s -> s.personId() == person.getId())) {
            throw new InvalidSheetException(person.getName() + " already holds a share of the claim");
        }
        if (Shares.total(shares).add(input.share()).compareTo(WHOLE) > 0) {
            throw new InvalidSheetException("The claim's shares cannot total more than 100.00");
        }
        holder.set(person);
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Share the loaded claim with another person on the sheet: not a SETTLED, VOID or DELETED claim, not with the
 *  claim's own person or a person already holding a share of it, and not so that its shares add up to more than
 *  100.00. */
public class SaveShare {
    public void save(@Val ShareInput input, @Val Claim loaded, PersonRepository people, ClaimShareRepository shares,
            SheetRepository sheets, Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException, PersonNotFoundException {
        if (loaded.status() == ClaimStatus.SETTLED || loaded.status() == ClaimStatus.VOID
                || loaded.status() == ClaimStatus.DELETED) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status() + " claim cannot be shared.");
        }
        Person with = people.find(loaded.sheetId(), input.personId());
        if (with == null) {
            throw new PersonNotFoundException(String.valueOf(input.personId()));
        }
        if (with.id() == loaded.personId()) {
            throw new ClaimStatusNotAllowedException("The claim is " + with.name() + "'s own; share it with someone else.");
        }
        List<ClaimShareRepository.Share> held = shares.of(loaded);
        if (held.stream().anyMatch(s -> s.personId() == with.id())) {
            throw new ClaimStatusNotAllowedException(with.name() + " already holds a share of this claim.");
        }
        BigDecimal total = held.stream().map(ClaimShareRepository.Share::pct).reduce(input.share(), BigDecimal::add);
        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ClaimStatusNotAllowedException("The claim's shares would add up to " + total.toPlainString()
                    + ", more than 100.00.");
        }
        shares.add(loaded, with.id(), input.share(), sheets.today(loaded.sheetId()));
        saved.set(ClaimResponse.from(loaded, List.of(), sheets.today(loaded.sheetId())).withShares(shares.of(loaded)));
    }
}

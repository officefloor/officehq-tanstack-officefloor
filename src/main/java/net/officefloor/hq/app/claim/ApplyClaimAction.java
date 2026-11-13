package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Moves the claim to the action's status; refused (400) when the claim's status does not allow it.
 * Opening gives the claim the sheet's next number; a voided claim keeps its number (it is never reused).
 * Opening records the user who opened it; that user cannot settle it (four eyes).
 * Settling fixes the claim's due at its person's grade's rate in force on the claim's date — or, when a statement
 * bound the claim, at the figure it was bound to; with no figure it stays live.
 */
public class ApplyClaimAction {
    public void apply(@PathVariable("action") String action, @RequestParam("user") String user, @Val Claim claim, SheetRepository sheets,
            PersonRepository people, RateRepository rates,
            Out<ClaimAction> applied, Out<ClaimNumber> number, Out<Claim> updated) throws InvalidClaimException {
        ClaimAction found = ClaimAction.of(action);
        if (found == null) {
            throw new InvalidClaimException("No such claim action: " + action);
        }
        if (!found.allowedFrom(claim.status())) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be " + found.to() + ".");
        }
        if (found == ClaimAction.SETTLE && user.equals(claim.openedBy())) {
            throw new InvalidClaimException("The user who opened a claim cannot settle it.");
        }
        Integer next = switch (found) {
            case OPEN -> sheets.nextClaimNumber(claim.sheetId());
            default -> claim.number();
        };
        applied.set(found);
        number.set(new ClaimNumber(next));
        BigDecimal due = found == ClaimAction.SETTLE ? settledDue(claim, people, rates) : claim.due();
        updated.set(claim.withStatus(found.to(), next, due, found == ClaimAction.OPEN ? user : claim.openedBy()));
    }

    private static BigDecimal settledDue(Claim claim, PersonRepository people, RateRepository rates) {
        if (claim.stated()) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        BigDecimal rate = person == null ? null
                : Rate.inForce(rates.findAll(claim.sheetId()), person.grade(), claim.date());
        return Claim.due(claim.amount(), rate);
    }
}

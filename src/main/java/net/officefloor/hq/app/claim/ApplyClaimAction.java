package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Moves the claim to the action's status; refused (400) when the claim's status does not allow it.
 * Opening gives the claim the sheet's next number; voiding clears it (the number is not reused).
 */
public class ApplyClaimAction {
    public void apply(@PathVariable("action") String action, @Val Claim claim, SheetRepository sheets,
            Out<ClaimAction> applied, Out<ClaimNumber> number, Out<Claim> updated) throws InvalidClaimException {
        ClaimAction found = ClaimAction.of(action);
        if (found == null) {
            throw new InvalidClaimException("No such claim action: " + action);
        }
        if (!found.allowedFrom(claim.status())) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be " + found.to() + ".");
        }
        Integer next = switch (found) {
            case OPEN -> sheets.nextClaimNumber(claim.sheetId());
            case VOID -> null;
            default -> claim.number();
        };
        applied.set(found);
        number.set(new ClaimNumber(found == ClaimAction.VOID ? claim.number() : next));
        updated.set(claim.withStatus(found.to(), next));
    }
}

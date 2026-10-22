package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). Opening gives the claim the sheet's next number; voiding
 *  clears it. */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!loaded.status().canBecome(input.to())) {
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        Integer number = switch (input.to()) {
            case OPEN -> repository.nextNumber(loaded.sheetId());
            case VOID -> null;
            default -> loaded.number();
        };
        if (!repository.changeStatus(loaded, input.to(), number)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), input.to(), number)));
    }
}

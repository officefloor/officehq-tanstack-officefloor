package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Refuses splitting a claim that is not OPEN (a PENDING one too), one deferred on the sheet's today, an
 *  amount not less than the claim's, or a split by a role the sheet's permission matrix does not allow to
 *  open a claim (the new claim is theirs to open). */
public class CheckSplit {
    public void check(@Val SplitInput input, @Val Claim claim, RoleStore roles, SettingStore settings, SheetStore sheets)
            throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new InvalidSheetException("Only an OPEN claim can be split");
        }
        if (claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
            throw new InvalidSheetException("A deferred claim cannot be split until " + claim.getDeferredUntil());
        }
        if (input.amount().compareTo(claim.getAmount()) >= 0) {
            throw new InvalidSheetException("The amount split off must be less than the claim's " + claim.getAmount());
        }
        CheckClerkLimit.refuseUnpermitted(ClaimStatus.OPEN, claim.getSheetId(),
                roles.find(claim.getSheetId(), input.user()), settings);
    }
}

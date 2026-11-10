package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Restores a DELETED claim to a DRAFT: only on the sheet's today it was deleted (its last DELETED
 * log line's date); on a later day the restore is refused.
 */
public class RestoreClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, ClaimLogRepository log) throws InvalidClaimStatusException {
        if (claim.getStatus() != ClaimStatus.DELETED) throw new InvalidClaimStatusException(claim.getStatus(), ClaimStatus.DRAFT);
        LocalDate deleted = log.deletedOn(claim);
        if (deleted == null || !deleted.equals(sheet.today())) {
            throw new InvalidClaimStatusException("A claim may be restored only on the day it was deleted"
                    + (deleted == null ? "" : ": " + deleted));
        }
        claim.moveTo(ClaimStatus.DRAFT);
    }
}

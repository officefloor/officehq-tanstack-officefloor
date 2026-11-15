package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Restores a DELETED claim to DRAFT, on the sheet's today it was deleted (its status log's last
 *  DELETED date). Refused for any other status, and on a later day. */
public class RestoreClaim {
    public void move(@Val Claim claim, ClaimLogRepository log, SheetRepository sheets) throws ClaimStatusException {
        ClaimStatus restored = ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.DRAFT);
        LocalDate deletedOn = log.deletedOn(claim.getSheetId(), claim.getId());
        if (deletedOn == null || !deletedOn.equals(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusException("A deleted claim can be restored only on the day it was deleted");
        }
        claim.setStatus(restored.name());
    }
}

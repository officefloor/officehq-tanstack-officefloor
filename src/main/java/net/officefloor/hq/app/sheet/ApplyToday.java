package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;

/** Changes the sheet's today; a change releases every claim lock on the sheet (each open editor). */
public class ApplyToday {
    public void apply(@Val LocalDate today, @Val Sheet sheet, SheetRepository repository, ClaimRepository claims) {
        if (!today.equals(sheet.getToday())) {
            for (Claim claim : claims.findBySheetIdAndLockedByIsNotNull(sheet.getId())) {
                claim.setLockedBy(null);
                claims.save(claim);
            }
        }
        sheet.setToday(today);
        repository.save(sheet);
    }
}

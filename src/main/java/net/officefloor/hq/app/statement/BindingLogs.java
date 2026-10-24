package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;

/** Records a person's claims bound now as a binding of theirs, on the sheet's today: done when bound claims move to
 *  them by a merge or a transfer, so a statement as of a later date finds them bound. */
public final class BindingLogs {

    private BindingLogs() {
    }

    public static void record(BindingLogRepository bindings, ClaimRepository claims, SheetRepository sheets,
            String sheetId, Long personId) {
        BindingLog binding = new BindingLog(sheetId, personId,
                sheets.findById(sheetId).map(Sheet::getToday).orElseGet(() -> new Sheet(sheetId).getToday()));
        for (Claim claim : claims.findBySheetIdAndPersonIdOrderByIdDesc(sheetId, personId)) {
            if (claim.isStated()) {
                binding.bind(claim.getId(), claim.getStatedDue());
            }
        }
        bindings.saveAndFlush(binding);
    }
}

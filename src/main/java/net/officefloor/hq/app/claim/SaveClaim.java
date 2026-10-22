package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Apply the edit to the loaded claim and store it; a cleared date becomes the sheet's today. */
public class SaveClaim {
    public void save(@Val ClaimInput input, @Val Claim loaded, SheetRepository sheets, ClaimRepository repository,
            Out<ClaimResponse> saved) {
        var date = input.date() != null ? input.date() : sheets.today(loaded.sheetId());
        Claim claim = new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), input.amount(), date, input.note(),
                loaded.status(), loaded.number(), loaded.settledDue(), loaded.openedBy(),
                loaded.stated(), loaded.statedDue(), input.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs());
        repository.update(claim);
        saved.set(ClaimResponse.from(claim));
    }
}

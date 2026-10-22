package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Defer the loaded claim until the date: only an OPEN claim, and only to a date after the sheet's today (a
 *  deferred claim may be deferred again). A deferral another user's change got to first is refused. */
public class SaveDefer {
    public void save(@Val DeferInput input, @Val Claim loaded, SheetRepository sheets, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException, InvalidClaimException {
        if (loaded.status() != ClaimStatus.OPEN) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be deferred; only an OPEN one can.");
        }
        var today = sheets.today(loaded.sheetId());
        if (today == null || !input.until().isAfter(today)) {
            throw new InvalidClaimException("Defer the claim to a date after the sheet's today (" + today + ").");
        }
        if (!repository.defer(loaded, input.until())) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(),
                input.until(), loaded.proofs()), List.of(), today));
    }
}

package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Toggle the loaded claim's hold: a claim not held is held, a held one released. Only an OPEN or PENDING claim may
 *  be; a toggle another user's change got to first is refused. */
public class SaveClaimHold {
    public void save(@Val ClaimHoldInput input, @Val Claim loaded, SheetRepository sheets, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        boolean held = !loaded.held();
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status() + " claim cannot be "
                    + (held ? "held" : "released") + "; only an OPEN or PENDING one can.");
        }
        if (!repository.hold(loaded, held)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(),
                loaded.deferredUntil(), loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(),
                loaded.ref(), loaded.tags(), loaded.waived(), loaded.photos(), held), List.of(),
                sheets.today(loaded.sheetId())));
    }
}

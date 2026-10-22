package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Raise the loaded claim's proofs count by one: not on a SETTLED or VOID claim. A proof another user's change got
 *  to first is refused. */
public class SaveProof {
    public void save(@Val ProofInput input, @Val Claim loaded, SheetRepository sheets, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (loaded.status() == ClaimStatus.SETTLED || loaded.status() == ClaimStatus.VOID) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status() + " claim cannot be given a proof.");
        }
        if (!repository.addProof(loaded)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(),
                loaded.deferredUntil(), loaded.proofs() + 1, loaded.splitFrom()), List.of(), sheets.today(loaded.sheetId())));
    }
}

package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Waive the loaded claim's late charge: only a steward may, only on an OPEN or PENDING claim, and only once. A waiver
 *  another user's change got to first is refused. */
public class SaveWaive {
    public void save(@Val WaiveInput input, @Val Claim loaded, RoleRepository roles, SheetRepository sheets,
            ClaimRepository repository, Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may waive a claim's late charge.");
        }
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim's late charge cannot be waived; only an OPEN or PENDING one's can.");
        }
        if (loaded.waived()) {
            throw new ClaimStatusNotAllowedException("The claim's late charge is already waived.");
        }
        if (!repository.waive(loaded)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(),
                loaded.deferredUntil(), loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(),
                loaded.ref(), loaded.tags(), true, loaded.photos(), loaded.held()), List.of(), sheets.today(loaded.sheetId())));
    }
}

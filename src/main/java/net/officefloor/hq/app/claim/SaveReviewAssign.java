package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Assign the loaded claim to a user to review: only a steward may (one by delegation too), and only a REVIEW claim
 *  (one already assigned may be assigned again). An assignment another user's change got to first is refused. */
public class SaveReviewAssign {
    public void save(@Val ReviewAssignInput input, @Val Claim loaded, RoleRepository roles, SheetRepository sheets,
            ClaimRepository repository, Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may assign a claim's review.");
        }
        if (loaded.status() != ClaimStatus.REVIEW) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim's review cannot be assigned; only a REVIEW one's can.");
        }
        if (!repository.assignReview(loaded, input.reviewer())) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(loaded, java.util.List.of(), sheets.today(loaded.sheetId()))
                .withReviewer(input.reviewer()));
    }
}

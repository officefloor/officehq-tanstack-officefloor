package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Applies an edit to a draft, recording the signed-in user as the one who last edited it. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, HttpServletRequest request)
            throws InvalidClaimStatusException {
        claim.checkEditable();
        claim.setAmount(input.amount());
        claim.setDate(input.date() == null ? sheet.today() : input.date());
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setEditedBy(Audit.user(request));
    }
}

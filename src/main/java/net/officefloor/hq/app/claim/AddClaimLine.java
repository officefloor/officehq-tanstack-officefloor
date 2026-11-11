package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.RoundingMode;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a line to a draft: an edit of the claim, so only a draft takes one, and the signed-in user is
 * recorded as the one who last edited it. The claim's amount becomes its lines' total.
 */
public class AddClaimLine {
    public void apply(@Val ClaimLine line, @Val Sheet sheet, @Val Claim claim, ClaimLineRepository lines,
            HttpServletRequest request) throws InvalidClaimStatusException {
        claim.checkEditable();
        lines.add(claim, sheet.today(), line);
        claim.setAmount(lines.total(claim).setScale(2, RoundingMode.HALF_UP));
        claim.setEditedBy(Audit.user(request));
    }
}

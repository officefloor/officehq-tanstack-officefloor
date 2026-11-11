package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Applies an edit to a draft, recording the signed-in user as the one who last edited it. A draft with
 * lines keeps their total as its amount: an edit typing another amount is refused.
 */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, ClaimLineRepository lines,
            HttpServletRequest request) throws InvalidClaimStatusException, InvalidClaimException {
        claim.checkEditable();
        BigDecimal total = lines.total(claim);
        if (total != null && total.compareTo(input.amount()) != 0) {
            throw new InvalidClaimException("A claim with lines takes their total as its amount");
        }
        claim.setAmount(input.amount());
        claim.setDate(input.date() == null ? sheet.today() : input.date());
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setEditedBy(Audit.user(request));
    }
}

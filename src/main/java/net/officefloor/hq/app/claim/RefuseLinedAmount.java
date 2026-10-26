package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** A claim with lines takes its amount from them: an edit that types another amount is refused. */
public class RefuseLinedAmount {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimLineRepository lines) throws BadRequestException {
        java.math.BigDecimal total = lines.total(claim.getId());
        if (total != null && total.compareTo(input.amount()) != 0) {
            throw new BadRequestException("This claim's amount is its lines' total, " + total.toPlainString());
        }
    }
}

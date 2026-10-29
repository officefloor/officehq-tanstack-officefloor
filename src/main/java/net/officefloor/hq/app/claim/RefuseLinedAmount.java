package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Val;

public class RefuseLinedAmount {
    // A claim with lines has their sum as its amount: an edit typing another amount is refused.
    public void refuse(@Val ClaimInput input, @Val Claim claim, Claims claims) throws ClaimLineRefusedException {
        List<ClaimLine> lines = claims.lines(claim);
        if (!lines.isEmpty() && input.amount().compareTo(ClaimLines.total(lines)) != 0) {
            throw new ClaimLineRefusedException("The claim's amount is its lines added up ("
                    + ClaimLines.total(lines).toPlainString() + "): it cannot be typed.");
        }
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the share's body once, publishing the share it asks for; refuses (400) one that names no person, or a percent
 * that is not a number with at most two decimals more than 0.00 and at most 100.00.
 */
public class ValidateShareRequest {
    public void validate(@PathVariable("claimId") long claimId, @RequestBody ShareRequest request,
            Out<ClaimShare> share) throws InvalidClaimException {
        if (request == null || request.personId() == null) {
            throw new InvalidClaimException("Choose the person to share the claim with.");
        }
        String typed = request.share() == null ? "" : request.share().trim();
        if (!typed.matches("\\d{1,3}(\\.\\d{1,2})?|\\.\\d{1,2}")) {
            throw new InvalidClaimException("Type the share as a percent with at most two decimals.");
        }
        BigDecimal pct = new BigDecimal(typed).setScale(2);
        if (pct.signum() <= 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidClaimException("A share is more than 0.00 and at most 100.00.");
        }
        share.set(new ClaimShare(claimId, request.personId(), pct));
    }
}

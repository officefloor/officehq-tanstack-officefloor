package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, a person to share the claim with and a
 *  percent with up to two decimals, above 0.00 and at most 100.00. */
public class ValidateShare {
    public void validate(@RequestBody ShareRequest request, Out<ShareInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        long personId;
        try {
            personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
        } catch (NumberFormatException e) {
            throw new InvalidClaimException("Choose the person to share the claim with.");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!share.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("The share must be a percent with up to two decimals.");
        }
        BigDecimal pct = new BigDecimal(share).setScale(2);
        if (pct.signum() <= 0 || pct.compareTo(new BigDecimal("100.00")) > 0) {
            throw new InvalidClaimException("The share must be more than 0.00 and at most 100.00.");
        }
        body.set(new ShareInput(request.user().trim(), personId, pct));
    }
}

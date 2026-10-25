package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the share from the body: a person must be chosen, and the share must be a number with up to two decimals,
 *  more than 0.00. */
public class ValidateShare {
    public void validate(@RequestBody ShareRequest request, Out<ShareRequest> share) throws InvalidClaimException {
        String person = request == null || request.personId() == null ? "" : request.personId().trim();
        String pct = request == null || request.share() == null ? "" : request.share().trim();
        if (!person.matches("\\d+")) {
            throw new InvalidClaimException("Choose a person to share the claim with.");
        }
        if (!pct.matches("\\d+(\\.\\d{1,2})?") || new BigDecimal(pct).signum() <= 0) {
            throw new InvalidClaimException("A share must be a number with up to two decimals, more than 0.00.");
        }
        share.set(new ShareRequest(person, new BigDecimal(pct).setScale(2).toPlainString()));
    }
}

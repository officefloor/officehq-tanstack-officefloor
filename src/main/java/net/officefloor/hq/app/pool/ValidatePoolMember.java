package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a person, and a share of at
 *  most two decimals, more than 0.00 and at most 100.00. */
public class ValidatePoolMember {

    private static final BigDecimal MAX = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> body)
            throws InvalidPoolException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPoolException("Sign in before adding a pool member.");
        }
        if (request.personId() == null) {
            throw new InvalidPoolException("Choose the person to add.");
        }
        String raw = request.share() == null ? "" : request.share().trim();
        if (!raw.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidPoolException("Give the share as a percent with at most two decimals.");
        }
        BigDecimal share = new BigDecimal(raw).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00.");
        }
        body.set(new PoolMemberInput(request.user().trim(), request.personId(), share));
    }
}

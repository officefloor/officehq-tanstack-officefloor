package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the member's person and share from the body; refuses a missing person (or pool), or a share that is not a number
 *  with at most two decimals, more than 0.00 and at most 100.00. */
public class ValidatePoolMember {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<ValidPoolMember> body) throws InvalidPoolException {
        if (request == null || (request.personId() == null) == (request.memberPoolId() == null)) {
            throw new InvalidPoolException("Choose a person or a pool for the pool.");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!share.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            throw new InvalidPoolException("A share is a percent with at most two decimals.");
        }
        BigDecimal value = new BigDecimal(share).setScale(2);
        if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
            throw new InvalidPoolException("A share must be more than 0.00 and at most 100.00.");
        }
        body.set(new ValidPoolMember(request.personId(), request.memberPoolId(), value));
    }
}

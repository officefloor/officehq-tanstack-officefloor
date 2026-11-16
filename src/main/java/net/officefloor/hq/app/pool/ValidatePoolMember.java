package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a pool and a person are named, and the share has at most two decimals, is more than 0.00 and at
 *  most 100.00. */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberRequest> body) throws InvalidPoolException {
        if (request == null || request.pool() == null || request.pool().trim().isEmpty() || request.personId() == null) {
            throw new InvalidPoolException("A pool member needs a pool and a person");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(share).matches()) {
            throw new InvalidPoolException("A share must be a number with at most two decimals");
        }
        BigDecimal value = new BigDecimal(share);
        if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
            throw new InvalidPoolException("A share must be more than 0.00 and at most 100.00");
        }
        body.set(request);
    }
}

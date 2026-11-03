package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the member form: a person or another pool must be chosen; the share is a percent with up to two decimals, more than
 *  0.00 and at most 100.00. */
public class ValidatePoolMember {
    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> member)
            throws BadRequestException {
        String memberPool = request == null || request.memberPool() == null ? "" : request.memberPool().trim();
        if (request == null || (request.personId() == null) == memberPool.isEmpty()) {
            throw new BadRequestException("choose a person or a pool");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!share.matches("\\d+(\\.\\d{1,2})?")) {
            throw new BadRequestException("share must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(share).setScale(2);
        if (value.signum() <= 0 || value.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BadRequestException("share must be more than 0.00 and at most 100.00");
        }
        member.set(new PoolMemberInput(request.pool() == null ? "" : request.pool(), request.personId(),
                memberPool.isEmpty() ? null : memberPool, value));
    }
}

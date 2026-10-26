package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A member to add: a pool, a person and a share percent (at most two decimals, more than 0.00, at most 100.00). */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> member, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.pool() == null || request.pool().isBlank()) {
            throw new BadRequestException("Choose a pool");
        }
        if (request.person() == null) {
            throw new BadRequestException("Choose a person");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new BadRequestException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("A share is more than 0.00 and at most 100.00");
        }
        member.set(new PoolMemberInput(request.pool().trim(), request.person(), share));
        changedBy.set(by);
    }
}

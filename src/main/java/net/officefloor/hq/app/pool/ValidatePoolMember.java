package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the member form: a person or another pool must be chosen; the share is a percent with up to two decimals, more than
 *  0.00 and at most 100.00; an optional from-date and to-date (yyyy-mm-dd), a to-date before the from-date refused. */
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
        LocalDate from = date(request.from(), "from-date");
        LocalDate to = date(request.to(), "to-date");
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("to-date must be on or after from-date");
        }
        member.set(new PoolMemberInput(request.pool() == null ? "" : request.pool(), request.personId(),
                memberPool.isEmpty() ? null : memberPool, value, from, to));
    }

    /** An optional date: null when blank. */
    private static LocalDate date(String value, String what) throws BadRequestException {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return null;
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(what + " must be yyyy-mm-dd");
        }
    }
}

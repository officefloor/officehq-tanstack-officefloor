package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A member needs a pool, a person or a member pool, and a share with at most two decimals, more than 0.00 and at most 100.00;
 * optional from- and to-dates, a to-date before the from-date refused. */
public class ValidatePoolMember {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> body) throws InvalidPoolException {
        if (request == null) throw new InvalidPoolException("A pool member is required");
        String pool = request.pool() == null ? "" : request.pool().trim();
        if (pool.isEmpty()) throw new InvalidPoolException("A pool is required");
        String memberPool = request.memberPool() == null ? "" : request.memberPool().trim();
        Long personId = null;
        if (memberPool.isEmpty()) {
            try {
                personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
            } catch (NumberFormatException e) {
                throw new InvalidPoolException("A person or a pool is required");
            }
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!text.matches("\\d+(\\.\\d{1,2})?")) throw new InvalidPoolException("A share is a number with at most two decimals");
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from(), "A from-date is a date");
        LocalDate to = date(request.to(), "A to-date is a date");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("A to-date is not before the from-date");
        }
        body.set(new PoolMemberInput(pool, personId, memberPool.isEmpty() ? null : memberPool, share, from, to));
    }

    private static LocalDate date(String value, String message) throws InvalidPoolException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException(message);
        }
    }
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the member's person and share from the body; refuses a missing person (or pool), or a share that is not a number
 *  with at most two decimals, more than 0.00 and at most 100.00, a date that is not YYYY-MM-DD, or a to-date before
 *  the from-date. */
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
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("A to-date must not be before the from-date.");
        }
        body.set(new ValidPoolMember(request.personId(), request.memberPoolId(), value, from, to));
    }

    private static LocalDate date(String value) throws InvalidPoolException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("A date must be YYYY-MM-DD.");
        }
    }
}

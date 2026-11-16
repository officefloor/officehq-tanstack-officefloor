package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a pool and either a person or a member pool are named, and the share has at most two decimals, is more than 0.00 and at
 *  most 100.00. Each date is blank or yyyy-MM-dd, and a to-date is not before the from-date. */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberRequest> body) throws InvalidPoolException {
        if (request == null || request.pool() == null || request.pool().trim().isEmpty()
                || (request.personId() == null) == (request.memberPool() == null || request.memberPool().trim().isEmpty())) {
            throw new InvalidPoolException("A pool member needs a pool and a person or a pool");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(share).matches()) {
            throw new InvalidPoolException("A share must be a number with at most two decimals");
        }
        BigDecimal value = new BigDecimal(share);
        if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
            throw new InvalidPoolException("A share must be more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("A membership's to-date must not be before its from-date");
        }
        body.set(request);
    }

    /** The date typed, or null when blank. */
    static LocalDate date(String value, String which) throws InvalidPoolException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("A membership's " + which + "-date must be yyyy-MM-dd");
        }
    }
}

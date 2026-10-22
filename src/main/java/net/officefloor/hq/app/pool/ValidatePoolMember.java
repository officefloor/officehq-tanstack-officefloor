package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a person or a pool, and a share of at
 *  most two decimals, more than 0.00 and at most 100.00, and from- and to-dates (each optional) that are dates, the
 *  to-date not before the from-date. */
public class ValidatePoolMember {

    private static final BigDecimal MAX = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> body)
            throws InvalidPoolException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPoolException("Sign in before adding a pool member.");
        }
        if ((request.personId() == null) == (request.memberPoolId() == null)) {
            throw new InvalidPoolException("Choose the person or pool to add.");
        }
        String raw = request.share() == null ? "" : request.share().trim();
        if (!raw.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidPoolException("Give the share as a percent with at most two decimals.");
        }
        BigDecimal share = new BigDecimal(raw).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("The to-date cannot be before the from-date.");
        }
        body.set(new PoolMemberInput(request.user().trim(), request.personId(), share, request.memberPoolId(), from,
                to));
    }

    /** The date as typed, or null when blank. */
    private static LocalDate date(String text, String which) throws InvalidPoolException {
        String raw = text == null ? "" : text.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("Give the " + which + "-date as a date (yyyy-mm-dd).");
        }
    }
}

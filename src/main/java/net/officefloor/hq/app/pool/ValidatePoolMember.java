package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A member to add: a pool, a person or another pool, and a share percent (at most two decimals, more than 0.00, at most 100.00), and
 * the membership's from-date and to-date (each optional; a to-date before the from-date is refused). */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@RequestBody PoolMemberRequest request, Out<PoolMemberInput> member, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.pool() == null || request.pool().isBlank()) {
            throw new BadRequestException("Choose a pool");
        }
        String memberPool = request.memberPool() == null ? "" : request.memberPool().trim();
        if (request.person() == null && memberPool.isEmpty()) {
            throw new BadRequestException("Choose a person or a pool");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new BadRequestException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("A share is more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("A membership's to-date cannot be before its from-date");
        }
        member.set(new PoolMemberInput(request.pool().trim(), request.person(),
                request.person() == null ? memberPool : null, share, from, to));
        changedBy.set(by);
    }

    private static LocalDate date(String value) throws BadRequestException {
        String date = value == null ? "" : value.trim();
        if (date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}

package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A rate's fields: a grade (A, B or C), a percent with up to two decimals, 0.00 to 100.00, and an
 * optional from-date (yyyy-mm-dd; blank for none).
 */
public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern RATE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> rate, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String text = request == null || request.rate() == null ? "" : request.rate().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("Choose a grade: A, B or C");
        }
        if (text.isEmpty()) {
            throw new BadRequestException("Enter a rate");
        }
        if (!RATE.matcher(text).matches()) {
            throw new BadRequestException("A rate is a percent, 0.00 to 100.00, with up to two decimals");
        }
        BigDecimal value = new BigDecimal(text).setScale(2);
        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("A rate is a percent, 0.00 to 100.00, with up to two decimals");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Enter a from-date as yyyy-mm-dd, or leave it blank");
            }
        }
        rate.set(new RateInput(grade, value, from));
        changedBy.set(by);
    }
}

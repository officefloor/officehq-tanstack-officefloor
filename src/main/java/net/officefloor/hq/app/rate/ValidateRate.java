package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** 0.00 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> body) throws InvalidSheetException {
        if (request == null || request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidSheetException("The rate must be a percent of 0.00 or more with up to two decimals");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = fromText.isEmpty() ? null : LocalDate.parse(fromText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + fromText);
        }
        body.set(new RateInput(request.grade(), new BigDecimal(pct).setScale(2), from, request.user()));
    }
}

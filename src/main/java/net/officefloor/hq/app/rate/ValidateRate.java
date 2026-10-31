package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** A percent: 0.00 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateRequest request,
            Out<RateRequest> body, Out<Rate> rate, Out<Actor> actor) throws InvalidSheetRequestException {
        if (request == null || request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetRequestException("The grade must be A, B or C");
        }
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidSheetRequestException("The rate must be a percent, 0.00 or more, with up to two decimals");
        }
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!rawFrom.isEmpty()) {
            try {
                from = LocalDate.parse(rawFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + rawFrom);
            }
        }
        actor.set(new Actor(request.user()));
        body.set(request);
        rate.set(new Rate(sheetId, request.grade(), new BigDecimal(pct).setScale(2), from));
    }
}

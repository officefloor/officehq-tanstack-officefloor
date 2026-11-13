package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateRequest request, Out<RateInput> body, RoleStore roles, GradeStore grades) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        body.set(check(request, grades.grades(sheetId.trim())));
    }

    /** The rate change the request asks for, checked with the rate form's rules against the sheet's grades. */
    static RateInput check(RateRequest request, List<String> grades) throws InvalidSheetException {
        if (request == null || request.grade() == null || !grades.contains(request.grade())) {
            throw new InvalidSheetException("The grade must be one of " + String.join(", ", grades));
        }
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidSheetException("The rate must be a percent of 0.00 or more with up to two decimals");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (to != null && from != null && to.isBefore(from)) {
            throw new InvalidSheetException("The to-date must not be before the from-date");
        }
        return new RateInput(request.grade(), new BigDecimal(pct).setScale(2), from, to, request.user());
    }

    /** The date (yyyy-MM-dd) of the text, null when blank. */
    private static LocalDate date(String value) throws InvalidSheetException {
        String text = value == null ? "" : value.trim();
        try {
            return text.isEmpty() ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimAmount;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A plan's fields: an amount by the claim form's rule (0.00 or more), a start date (blank: the sheet's today), every N
 * days (a whole number, at least 1) and an end date (required, not before the start). Publishes them parsed, with the
 * user making the change.
 */
public class ValidatePlan {

    static final String END_BEFORE_START = "A plan's end date is before its start date";

    public void validate(@RequestBody PlanRequest request, Out<PlanInput> plan, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        BigDecimal amount = ClaimAmount.of(request == null ? null : request.amount());
        String startText = request == null || request.start() == null ? "" : request.start().trim();
        LocalDate start = startText.isEmpty() ? null : date(startText);
        String everyText = request == null || request.every() == null ? "" : request.every().trim();
        if (!everyText.matches("\\d{1,6}") || Integer.parseInt(everyText) < 1) {
            throw new BadRequestException("Every is a whole number of days, at least 1");
        }
        String endText = request == null || request.end() == null ? "" : request.end().trim();
        if (endText.isEmpty()) {
            throw new BadRequestException("Enter a plan's end date");
        }
        LocalDate end = date(endText);
        if (start != null && end.isBefore(start)) {
            throw new BadRequestException(END_BEFORE_START);
        }
        plan.set(new PlanInput(amount, start, Integer.parseInt(everyText), end));
        changedBy.set(by);
    }

    private static LocalDate date(String text) throws BadRequestException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}

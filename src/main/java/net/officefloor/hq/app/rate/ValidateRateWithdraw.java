package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: only a steward may withdraw a rate; the from-date is blank (none) or a date yyyy-MM-dd. */
public class ValidateRateWithdraw {
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("grade") String grade,
            @RequestBody RateWithdrawRequest request, RoleRepository roles, Out<RateWithdrawValues> values)
            throws InvalidRateException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheetId.trim(), by))) {
            throw new InvalidRateException("Only a steward may withdraw a rate");
        }
        String text = request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = text.isEmpty() || text.equals("start") ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("The from-date must be a date yyyy-MM-dd");
        }
        values.set(new RateWithdrawValues(grade == null ? "" : grade.trim(), from, by));
    }
}

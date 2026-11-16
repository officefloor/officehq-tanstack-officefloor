package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Refused, with nothing written, for anyone but a steward, and when the sheet has no rate of its own
 *  for the grade and from-date. */
public class ValidateRateWithdrawal {
    public void validate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @RequestBody WithdrawRateRequest request,
            RoleRepository roles, RateRepository rates, Out<RateWithdrawal> body) throws InvalidRateException {
        String id = sheetId.trim();
        if (!Role.STEWARD.equals(roles.roleOf(id, by == null ? "" : by))) {
            throw new InvalidRateException("Only a steward can withdraw a rate");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String text = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = text.isEmpty() || text.equals("start") ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a date: " + text);
        }
        if (!rates.has(id, grade, from)) {
            throw new InvalidRateException("No rate to withdraw");
        }
        body.set(new RateWithdrawal(id, grade, from));
    }
}

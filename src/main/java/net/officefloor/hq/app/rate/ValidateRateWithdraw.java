package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a rate withdrawal by anyone but a steward, or of a rate the sheet does not have. */
public class ValidateRateWithdraw {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateWithdrawRequest request,
            RoleStore roles, RateStore rates, Out<RateWithdrawInput> body) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may withdraw a rate");
        }
        String text = request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = text.isEmpty() ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
        if (request.grade() == null || rates.version(sheet, request.grade(), from) == 0) {
            throw new InvalidSheetException("There is no such rate to withdraw");
        }
        body.set(new RateWithdrawInput(request.grade(), from, user));
    }
}

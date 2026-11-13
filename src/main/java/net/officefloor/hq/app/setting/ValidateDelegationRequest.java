package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a delegation made by anyone but a steward (a steward given the role, not one by delegation), one with
 * a blank user name, dates not yyyy-mm-dd or a to-date before its from-date; a blank date is the sheet's today.
 */
public class ValidateDelegationRequest {
    public void validate(@Val Sheet sheet, @RequestParam("user") String user, @RequestBody @Valid DelegationRequest request,
            SettingRepository settings, Out<Delegation> delegation) throws InvalidSettingException {
        if (settings.find(sheet.id()).givenRoleOf(user) != Role.STEWARD) {
            throw new InvalidSettingException("Only a steward can delegate the steward role.");
        }
        String delegate = request.user() == null ? "" : request.user().trim();
        if (delegate.isEmpty()) {
            throw new InvalidSettingException("A user name is required.");
        }
        LocalDate from = date(request.from(), sheet.today());
        LocalDate to = date(request.to(), sheet.today());
        if (to.isBefore(from)) {
            throw new InvalidSettingException("The to-date cannot be before the from-date.");
        }
        delegation.set(new Delegation(sheet.id(), delegate, from, to, user));
    }

    private static LocalDate date(String value, LocalDate today) throws InvalidSettingException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return today;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("A delegation's dates must be yyyy-mm-dd.");
        }
    }
}

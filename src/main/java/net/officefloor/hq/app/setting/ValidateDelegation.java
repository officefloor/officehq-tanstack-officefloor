package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward (by the role set, not by a delegation) delegates; the to-date is not before the from-date. */
public class ValidateDelegation {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DelegationRequest request,
            Out<ValidDelegation> body, Settings settings, Sheets sheets) throws InvalidSettingException {
        if (sheets.archived(sheetId)) {
            throw new InvalidSettingException("The sheet is archived: nothing on it can change");
        }
        if (!Settings.STEWARD.equals(settings.roleSet(sheetId, request.user()))) {
            throw new InvalidSettingException("Only a steward can delegate the steward role");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSettingException("A delegation needs a user name");
        }
        LocalDate today = sheets.today(sheetId);
        LocalDate from = date(request.from(), today);
        LocalDate to = date(request.to(), today);
        if (to.isBefore(from)) {
            throw new InvalidSettingException("The to-date cannot be before the from-date");
        }
        body.set(new ValidDelegation(name, from, to, request.user().trim()));
    }

    private static LocalDate date(String value, LocalDate blank) throws InvalidSettingException {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return blank;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("A delegation's dates must be dates (YYYY-MM-DD)");
        }
    }
}

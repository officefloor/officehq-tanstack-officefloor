package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward by their own role (not one by delegation) may delegate, and never with a to-date
 *  before the from-date. */
public class ValidateDelegation {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DelegationRequest request,
            Settings settings, Sheets sheets, Out<Delegation> delegation) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.contains(" ")) {
            throw new InvalidSheetRequestException("The steward role is delegated to a user name");
        }
        String by = request.user() == null ? "" : request.user().trim();
        if (settings.ownRole(sheetId, by) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can delegate the steward role");
        }
        LocalDate from = date(request.from(), sheets.today(sheetId));
        LocalDate to = date(request.to(), from);
        if (to.isBefore(from)) {
            throw new InvalidSheetRequestException("The to-date is on or after the from-date");
        }
        delegation.set(new Delegation(sheetId, name, from, to, by));
    }

    private static LocalDate date(String value, LocalDate blank) throws InvalidSheetRequestException {
        if (value == null || value.isBlank()) return blank;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("A date is yyyy-mm-dd: " + value);
        }
    }
}

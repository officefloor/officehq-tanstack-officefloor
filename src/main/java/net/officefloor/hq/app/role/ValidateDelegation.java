package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a delegation by anyone but a steward (one by role, not by a delegation), without a user, or
 *  with a to-date before the from-date. */
public class ValidateDelegation {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DelegationRequest request,
            Out<Delegation> body, RoleStore roles, SheetStore sheets) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (roles.assigned(sheet, by) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can delegate the steward role");
        }
        String user = request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSheetException("A user name is required");
        }
        LocalDate from = date(request.from(), sheets.today(sheet), "from-date");
        LocalDate to = date(request.to(), null, "to-date");
        if (to.isBefore(from)) {
            throw new InvalidSheetException("The to-date cannot be before the from-date");
        }
        body.set(new Delegation(user, from, to, by));
    }

    private static LocalDate date(String text, LocalDate blank, String what) throws InvalidSheetException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            if (blank == null) {
                throw new InvalidSheetException("A " + what + " is required");
            }
            return blank;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("The " + what + " must be a date (yyyy-mm-dd)");
        }
    }
}

package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: only a steward (given the role, not one by delegation) delegates; the user is not blank; the dates
 *  are yyyy-MM-dd (a blank from-date is the sheet's today, a blank to-date the from-date) and the to-date is not before
 *  the from-date. */
public class ValidateDelegation {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DelegationRequest request,
            RoleRepository roles, SheetRepository sheets, Out<DelegationValues> values)
            throws InvalidDelegationException {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (!RoleRepository.STEWARD.equals(roles.givenRoleOf(sheet, by))) {
            throw new InvalidDelegationException("Only a steward may delegate the steward role");
        }
        String user = request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidDelegationException("The user name must not be blank");
        }
        LocalDate from = date(request.from(), sheets.today(sheet), "from");
        LocalDate to = date(request.to(), from, "to");
        if (to.isBefore(from)) {
            throw new InvalidDelegationException("The to-date may not be before the from-date");
        }
        values.set(new DelegationValues(user, from, to, by));
    }

    private static LocalDate date(String text, LocalDate blank, String which) throws InvalidDelegationException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return blank;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new InvalidDelegationException("The " + which + "-date must be a date yyyy-MM-dd");
        }
    }
}

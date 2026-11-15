package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Refused for anyone but a steward (by their own role: a delegated steward cannot delegate), without a
 *  user name, for a date that is not one, and for a to-date before the from-date. */
public class ValidateDelegation {
    public void validate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody DelegationRequest request, SheetRepository sheets, RoleRepository roles,
            Out<DelegationRequest> body) throws InvalidSettingException {
        String id = sheetId.trim();
        if (!Role.STEWARD.equals(roles.assignedRoleOf(id, by == null ? "" : by))) {
            throw new InvalidSettingException("Only a steward can delegate");
        }
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSettingException("A user name is required");
        }
        LocalDate today = sheets.today(id);
        LocalDate from = date(request.from(), today);
        LocalDate to = date(request.to(), today);
        if (to.isBefore(from)) {
            throw new InvalidSettingException("The to-date is before the from-date");
        }
        body.set(new DelegationRequest(user, from.toString(), to.toString()));
    }

    private static LocalDate date(String value, LocalDate today) throws InvalidSettingException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return today;
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("Not a date: " + text);
        }
    }
}

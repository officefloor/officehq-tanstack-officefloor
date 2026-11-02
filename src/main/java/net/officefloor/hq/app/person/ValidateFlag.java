package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Only a steward flags a person, with a reason (not blank) until the date given (the sheet's today when blank). */
public class ValidateFlag {
    public void validate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody FlagRequest request, Roles roles, SheetClock clock, Out<Flag> flag)
            throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward may flag a person");
        }
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        String until = request == null || request.until() == null ? "" : request.until().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason");
        }
        LocalDate date;
        try {
            date = until.isEmpty() ? clock.today(sheet) : LocalDate.parse(until);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date: " + until);
        }
        flag.set(new Flag(reason, date));
    }
}

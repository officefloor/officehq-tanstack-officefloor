package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a flag by anyone but a steward, with a blank reason, or with an until-date that is not a date. */
public class ValidateFlag {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody FlagRequest request,
            SheetStore sheets, RoleStore roles, Out<FlagInput> body) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can flag a person");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidSheetException("A flag needs a reason");
        }
        String untilText = request.until() == null ? "" : request.until().trim();
        LocalDate until;
        try {
            until = untilText.isEmpty() ? sheets.today(sheet) : LocalDate.parse(untilText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + untilText);
        }
        body.set(new FlagInput(new Flag(reason, until), user));
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward may flag a person, and only with a reason and an until-date. */
public class ValidateFlag {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody FlagRequest request, Settings settings,
            Out<FlagChange> body, Out<Actor> actor) throws InvalidSheetRequestException {
        String user = request != null ? request.user() : null;
        if (user == null || settings.role(sheetId, user) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can flag a person");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) throw new InvalidSheetRequestException("A flag needs a reason");
        String raw = request.until() == null ? "" : request.until().trim();
        LocalDate until;
        try {
            until = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
        body.set(new FlagChange(reason, until, user));
        actor.set(new Actor(user));
    }
}

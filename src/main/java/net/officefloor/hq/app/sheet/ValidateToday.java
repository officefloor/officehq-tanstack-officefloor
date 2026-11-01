package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Validates a change of the sheet's today: a date, by a role the permission matrix allows to change
 *  it (see {@link Settings#allows}, action today). */
public class ValidateToday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TodayRequest request, Settings settings,
            Out<LocalDate> today, Out<Actor> actor) throws InvalidSheetRequestException {
        String raw = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(raw));
            actor.set(new Actor(request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
        String user = request.user() == null ? "" : request.user().trim();
        if (!settings.allows(sheetId, "today", user)) {
            throw new InvalidSheetRequestException("Your role may not change the sheet's today");
        }
    }
}

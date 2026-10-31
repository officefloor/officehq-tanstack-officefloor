package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String raw = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(raw));
            actor.set(new Actor(request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

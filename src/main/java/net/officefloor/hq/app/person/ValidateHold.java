package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldChange> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request != null ? request.user() : null;
        LocalDate from = date(request != null ? request.from() : null);
        LocalDate to = date(request != null ? request.to() : null);
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidSheetRequestException("A hold's to-date cannot be before its from-date");
        }
        body.set(new HoldChange(from, to, user));
        actor.set(new Actor(user));
    }

    private static LocalDate date(String typed) throws InvalidSheetRequestException {
        String raw = typed == null ? "" : typed.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

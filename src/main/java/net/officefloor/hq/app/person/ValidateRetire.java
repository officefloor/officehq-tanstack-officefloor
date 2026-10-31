package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<Retirement> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        Retirement retirement = checked(request);
        body.set(retirement);
        actor.set(new Actor(retirement.user()));
    }

    /** The retirement as typed, checked: a blank date is the sheet's today, anything else a date. */
    static Retirement checked(RetireRequest request) throws InvalidSheetRequestException {
        String raw = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!raw.isEmpty()) {
            try {
                from = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
            }
        }
        return new Retirement(from, request == null ? null : request.user());
    }
}

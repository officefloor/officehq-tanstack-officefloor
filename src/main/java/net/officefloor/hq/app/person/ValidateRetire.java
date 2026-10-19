package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the retire request; a blank date stays empty here, to default to the sheet's today. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<Retirement> retirement) throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().strip();
        if (from.isEmpty()) {
            retirement.set(new Retirement(null));
            return;
        }
        try {
            retirement.set(new Retirement(LocalDate.parse(from)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date.");
        }
    }
}

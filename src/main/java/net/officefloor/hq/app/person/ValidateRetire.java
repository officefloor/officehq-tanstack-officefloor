package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the date is blank or yyyy-MM-dd. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<Retirement> retirement) throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate parsed = null;
        if (!from.isEmpty()) {
            try {
                parsed = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The retirement date must be yyyy-MM-dd");
            }
        }
        retirement.set(new Retirement(parsed, request == null || request.by() == null ? "" : request.by().trim()));
    }
}

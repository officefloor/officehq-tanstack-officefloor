package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireInput> body)
            throws InvalidPersonException {
        String raw = request == null || request.from() == null ? "" : request.from().trim();
        if (raw.isEmpty()) {
            body.set(new RetireInput(null));
            return;
        }
        try {
            body.set(new RetireInput(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("A retirement date is a date");
        }
    }
}

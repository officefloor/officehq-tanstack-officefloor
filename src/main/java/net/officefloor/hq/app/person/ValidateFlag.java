package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<FlagInput> body)
            throws InvalidPersonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) throw new InvalidPersonException("A flag needs a reason");
        String raw = request.until() == null ? "" : request.until().trim();
        if (raw.isEmpty()) {
            body.set(new FlagInput(reason, null));
            return;
        }
        try {
            body.set(new FlagInput(reason, LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("A flag's until-date is a date");
        }
    }
}

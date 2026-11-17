package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the reason is not blank, the until-date blank or yyyy-MM-dd. */
public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<Flag> flag) throws InvalidPersonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason");
        }
        String text = request.until() == null ? "" : request.until().trim();
        LocalDate until = null;
        if (!text.isEmpty()) {
            try {
                until = LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The flag's until-date must be yyyy-MM-dd");
            }
        }
        flag.set(new Flag(reason, until, request.by() == null ? "" : request.by().trim()));
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the flag form: a reason is required (one word or more, kept as typed less outer spaces), and an until-date. */
public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<FlagInput> flag) throws BadRequestException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new BadRequestException("give a reason to flag the person");
        }
        if (reason.length() > 200) {
            throw new BadRequestException("the reason must be at most 200 characters");
        }
        String raw = request.until() == null ? "" : request.until().trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("give the date the flag is in force until");
        }
        try {
            flag.set(new FlagInput(reason, LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("until must be yyyy-mm-dd");
        }
    }
}

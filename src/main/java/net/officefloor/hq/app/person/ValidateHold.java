package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A blank from-date takes the sheet's today (filled in when stored); a blank to-date leaves the hold open-ended.
    // A to-date before the from-date is checked once the from-date is known.
    public void validate(@RequestBody HoldRequest body, Out<HoldDates> dates) throws InvalidPersonException {
        LocalDate from = parse(body == null ? null : body.from());
        LocalDate to = parse(body == null ? null : body.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPersonException("The to-date is before the from-date.");
        }
        dates.set(new HoldDates(from, to));
    }

    private static LocalDate parse(String value) throws InvalidPersonException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw, ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

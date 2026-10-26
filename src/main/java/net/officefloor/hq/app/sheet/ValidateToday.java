package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // Blank takes the default: the real date.
    public LocalDate validate(@RequestBody TodayRequest body) throws InvalidTodayException {
        String raw = body == null || body.today() == null ? "" : body.today().trim();
        if (raw.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(raw, ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

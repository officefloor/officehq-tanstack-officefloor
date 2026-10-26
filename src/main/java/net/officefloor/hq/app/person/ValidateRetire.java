package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A blank date takes the sheet's today (filled in when stored).
    public void validate(@RequestBody RetireRequest body, Out<RetireFrom> from) throws InvalidPersonException {
        String raw = body == null || body.from() == null ? "" : body.from().trim();
        if (raw.isEmpty()) {
            from.set(new RetireFrom(null));
            return;
        }
        try {
            from.set(new RetireFrom(LocalDate.parse(raw, ISO)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public void validate(@RequestBody PeriodRequest request, Out<ValidPeriod> body) throws InvalidPeriodException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A period needs a name");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("The from-date must be on or before the to-date");
        }
        body.set(new ValidPeriod(name, from, to, request.user()));
    }

    private static LocalDate date(String raw, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(raw == null ? "" : raw.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (YYYY-MM-DD)");
        }
    }
}

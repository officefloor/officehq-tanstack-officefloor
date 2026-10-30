package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidatePeriod {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PeriodRequest request, Out<ValidPeriod> body, Settings settings)
            throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A period needs a name");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("The from-date must be on or before the to-date");
        }
        body.set(new ValidPeriod(name, from, to, request.user(), cap(request.cap())));
    }

    /** The cap: blank for none, else a whole number of at least 1. */
    private static Integer cap(String raw) throws InvalidPeriodException {
        String cap = raw == null ? "" : raw.trim();
        if (cap.isEmpty()) {
            return null;
        }
        if (!cap.matches("[0-9]{1,9}") || Integer.parseInt(cap) < 1) {
            throw new InvalidPeriodException("The cap must be a whole number of at least 1, or blank for none");
        }
        return Integer.parseInt(cap);
    }

    private static LocalDate date(String raw, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(raw == null ? "" : raw.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (YYYY-MM-DD)");
        }
    }
}

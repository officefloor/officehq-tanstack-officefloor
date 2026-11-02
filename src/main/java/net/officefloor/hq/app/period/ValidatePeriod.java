package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the period form: the name is required (no spaces, as it names the period's row and audit
 *  field), the from- and to-date are yyyy-mm-dd and from is on or before to. */
public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> period) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        if (name.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("name must not contain spaces");
        }
        if (name.length() > 500) {
            throw new BadRequestException("name is too long");
        }
        LocalDate from = date(request.from(), "from-date");
        LocalDate to = date(request.to(), "to-date");
        if (from.isAfter(to)) {
            throw new BadRequestException("from-date must be on or before to-date");
        }
        period.set(new PeriodInput(name, from, to));
    }

    private static LocalDate date(String value, String what) throws BadRequestException {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new BadRequestException(what + " is required");
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(what + " must be yyyy-mm-dd");
        }
    }
}

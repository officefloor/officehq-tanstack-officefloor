package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a retirement date that is not yyyy-mm-dd; a blank one is the sheet's today. */
public class ValidateRetireRequest {
    public void validate(@RequestBody @Valid RetireRequest request, Out<Retirement> retirement)
            throws InvalidPersonException {
        String from = request.from() == null ? "" : request.from().trim();
        if (from.isEmpty()) {
            retirement.set(new Retirement(null));
            return;
        }
        try {
            retirement.set(new Retirement(LocalDate.parse(from)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The retirement date must be yyyy-mm-dd.");
        }
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the retire form: a blank date takes the sheet's today. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireInput> retire) throws BadRequestException {
        String raw = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate on = null;
        if (!raw.isEmpty()) {
            try {
                on = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("date must be yyyy-mm-dd");
            }
        }
        retire.set(new RetireInput(on));
    }
}

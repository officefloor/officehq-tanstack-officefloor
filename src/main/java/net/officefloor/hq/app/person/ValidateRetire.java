package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<Retirement> body)
            throws InvalidSheetRequestException {
        String raw = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!raw.isEmpty()) {
            try {
                from = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
            }
        }
        body.set(new Retirement(from, request == null ? null : request.user()));
    }
}

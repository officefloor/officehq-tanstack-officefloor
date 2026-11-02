package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today) throws BadRequestException {
        String raw = request == null || request.date() == null ? "" : request.date().trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("date is required");
        }
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("date must be yyyy-mm-dd");
        }
    }
}

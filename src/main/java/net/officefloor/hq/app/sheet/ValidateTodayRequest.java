package net.officefloor.hq.app.sheet;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTodayRequest {
    public void validate(@RequestBody @Valid TodayRequest request, Out<LocalDate> today) throws InvalidTodayException {
        String value = request.today() == null ? "" : request.today().trim();
        if (value.isEmpty()) {
            today.set(LocalDate.now()); // the default: the real date
            return;
        }
        try {
            today.set(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException(value);
        }
    }
}

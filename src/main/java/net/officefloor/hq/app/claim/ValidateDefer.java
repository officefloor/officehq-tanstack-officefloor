package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<DeferInput> body) throws InvalidSheetException {
        String text = request == null || request.until() == null ? "" : request.until().trim();
        try {
            body.set(new DeferInput(LocalDate.parse(text), request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the due-by's body once, publishing the new due-by; refuses (400) a missing or malformed date. */
public class ValidateDueByRequest {
    public void validate(@RequestBody DueByRequest request, Out<NewDueByDate> dueBy) throws InvalidClaimException {
        String typed = request == null || request.dueBy() == null ? "" : request.dueBy().trim();
        if (typed.isEmpty()) {
            throw new InvalidClaimException("Type the claim's new due-by.");
        }
        try {
            dueBy.set(new NewDueByDate(LocalDate.parse(typed)));
        } catch (DateTimeParseException ex) {
            throw new InvalidClaimException("Type a date as yyyy-mm-dd.");
        }
    }
}

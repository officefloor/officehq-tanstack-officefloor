package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the due-by body once, refusing one that names no valid date. */
public class ValidateDueBy {
    public void validate(@RequestBody DueByRequest request, Out<LocalDate> dueBy)
            throws InvalidClaimStatusChangeException {
        if (request == null || request.dueBy() == null || request.dueBy().isBlank()) {
            throw new InvalidClaimStatusChangeException("A due-by set names the claim's new due-by");
        }
        try {
            dueBy.set(LocalDate.parse(request.dueBy().trim()));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusChangeException("A due-by is yyyy-mm-dd");
        }
    }
}

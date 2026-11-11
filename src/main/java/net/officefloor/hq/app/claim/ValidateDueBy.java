package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Setting a due-by needs the claim's new due-by. */
public class ValidateDueBy {
    public void validate(@RequestBody DueByRequest request, Out<DueBy> body) throws InvalidClaimException {
        String raw = request == null || request.dueBy() == null ? "" : request.dueBy().trim();
        try {
            body.set(new DueBy(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Setting a due-by needs the claim's new due-by");
        }
    }
}

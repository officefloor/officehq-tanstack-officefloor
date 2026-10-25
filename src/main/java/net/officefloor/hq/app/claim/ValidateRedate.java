package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The date to re-date the claim to. Publishes it with the user re-dating it. */
public class ValidateRedate {
    public void validate(@RequestBody RedateRequest request, Out<RedateTo> to, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.date() == null ? "" : request.date().trim();
        try {
            to.set(new RedateTo(LocalDate.parse(text)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter the claim's new date as yyyy-mm-dd");
        }
        changedBy.set(by);
    }
}

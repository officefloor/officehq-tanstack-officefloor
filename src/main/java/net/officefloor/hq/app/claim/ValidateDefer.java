package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The date to defer the claim until. Publishes it with the user deferring it. */
public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<DeferUntil> until, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.until() == null ? "" : request.until().trim();
        try {
            until.set(new DeferUntil(LocalDate.parse(text)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter the date to defer until as yyyy-mm-dd");
        }
        changedBy.set(by);
    }
}

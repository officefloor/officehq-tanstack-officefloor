package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A flag's reason (not blank) and until date. Publishes them parsed, with the user making the change. */
public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<FlagInput> flag, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new BadRequestException("Enter a reason for the flag");
        }
        String until = request.until() == null ? "" : request.until().trim();
        if (until.isEmpty()) {
            throw new BadRequestException("Enter the date the flag holds until");
        }
        try {
            flag.set(new FlagInput(reason, LocalDate.parse(until)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
        changedBy.set(by);
    }
}

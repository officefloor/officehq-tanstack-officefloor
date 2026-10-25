package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A retirement's date (blank for the sheet's today). Publishes it parsed, with the user making the change. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireInput> retire, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String date = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Enter a date as YYYY-MM-DD");
            }
        }
        retire.set(new RetireInput(parsed));
        changedBy.set(by);
    }
}

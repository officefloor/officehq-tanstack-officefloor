package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Publishes the user taking the statement, and the date it is taken as of (blank: now). */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<ChangedBy> changedBy, Out<StatementAsOf> asOf)
            throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
        String text = request == null || request.asOf() == null ? "" : request.asOf().trim();
        try {
            asOf.set(new StatementAsOf(text.isEmpty() ? null : LocalDate.parse(text)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as yyyy-mm-dd");
        }
    }
}

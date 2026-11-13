package net.officefloor.hq.app.statement;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a statement whose as-of date is not yyyy-mm-dd; a blank one takes the statement now. */
public class ValidateStatementRequest {
    public void validate(@RequestBody @Valid StatementRequest request, Out<StatementAsOf> asOf)
            throws InvalidStatementException {
        String value = request == null || request.asof() == null ? "" : request.asof().trim();
        if (value.isEmpty()) {
            asOf.set(new StatementAsOf(null));
            return;
        }
        try {
            asOf.set(new StatementAsOf(LocalDate.parse(value)));
        } catch (DateTimeParseException e) {
            throw new InvalidStatementException("A statement's date must be yyyy-mm-dd.");
        }
    }
}

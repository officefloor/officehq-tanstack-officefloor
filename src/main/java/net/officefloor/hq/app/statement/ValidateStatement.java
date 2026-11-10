package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementInput> body)
            throws InvalidStatementException {
        String raw = request == null || request.asOf() == null ? "" : request.asOf().trim();
        if (raw.isEmpty()) {
            body.set(new StatementInput(null));
            return;
        }
        try {
            body.set(new StatementInput(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidStatementException("A statement's date is a date");
        }
    }
}

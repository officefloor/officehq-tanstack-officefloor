package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the statement form: a blank as-of date takes the statement now. */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementInput> statement)
            throws BadRequestException {
        String raw = request == null || request.asof() == null ? "" : request.asof().trim();
        LocalDate asof = null;
        if (!raw.isEmpty()) {
            try {
                asof = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("the statement date must be yyyy-mm-dd");
            }
        }
        statement.set(new StatementInput(asof));
    }
}

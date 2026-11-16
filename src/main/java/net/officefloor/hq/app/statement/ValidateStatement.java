package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who takes the statement, and the date it is taken as of (blank: the sheet's today; one not
 *  yyyy-MM-dd is refused). */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementTaker> taker) throws InvalidPersonException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String asOf = request == null || request.asOf() == null ? "" : request.asOf().trim();
        LocalDate date = null;
        if (!asOf.isEmpty()) {
            try {
                date = LocalDate.parse(asOf);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The statement date must be yyyy-MM-dd, or blank");
            }
        }
        taker.set(new StatementTaker(by, date));
    }
}

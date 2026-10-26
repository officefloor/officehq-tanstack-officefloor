package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatement {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A blank as-of date takes the statement now; one after the sheet's today is checked once the person is loaded.
    public void validate(@RequestBody StatementRequest body, Out<StatementAsOf> asOf) throws StatementRefusedException {
        String raw = body == null || body.asOf() == null ? "" : body.asOf().trim();
        if (raw.isEmpty()) {
            asOf.set(new StatementAsOf(null));
            return;
        }
        try {
            asOf.set(new StatementAsOf(LocalDate.parse(raw, ISO)));
        } catch (DateTimeParseException e) {
            throw new StatementRefusedException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

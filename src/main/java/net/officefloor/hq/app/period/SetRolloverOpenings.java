package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SetRolloverOpenings {
    // Each person's open figure at the old year's end (any above 0.00) is added to their opening balance for the new
    // year's first period, in sheet order (OPENING_SET); then the rollover itself is recorded (ROLLOVER).
    public void set(@PathVariable("sheetId") String sheetId, @Val Rollover rollover, @Val GeneratedPeriods generated,
            Periods periods, People people, ServerHttpConnection connection, Audit audit) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        if (rollover.first() != null) {
            for (Person person : people.list(sheet)) {
                BigDecimal amount = rollover.openings().get(person.id());
                if (amount == null || amount.signum() <= 0) {
                    continue;
                }
                periods.addOpening(sheet, rollover.first().name(), person.id(), amount);
                audit.record(Audit.line("OPENING_SET", sheet, user, "name=" + person.name(),
                        "period=" + rollover.first().name(), "amount=" + amount.toPlainString()));
            }
        }
        audit.record(Audit.line("ROLLOVER", sheet, user, "fy=" + rollover.fy(),
                "periods=" + generated.periods().size()));
    }
}

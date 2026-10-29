package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SetRolloverOpenings {
    // Each person's open figure at the old year's end (any above 0.00) is added to their opening balance for the new
    // year's first period, in sheet order (OPENING_SET), unless the year was rolled over already; then the rollover itself is kept (for undoing) and recorded (ROLLOVER).
    public void set(@PathVariable("sheetId") String sheetId, @Val Rollover rollover, @Val GeneratedPeriods generated,
            Periods periods, People people, Rollovers rollovers, Sheets sheets, ServerHttpConnection connection,
            Audit audit) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        Map<Long, BigDecimal> set = new LinkedHashMap<>();
        // A year rolled over again (its last rollover not undone) carries nothing a second time.
        if (rollover.first() != null && !rollovers.rolled(sheet, rollover.fy())) {
            for (Person person : people.list(sheet)) {
                BigDecimal amount = rollover.openings().get(person.id());
                if (amount == null || amount.signum() <= 0) {
                    continue;
                }
                periods.addOpening(sheet, rollover.first().name(), person.id(), amount);
                set.put(person.id(), amount);
                audit.record(Audit.line("OPENING_SET", sheet, user, "name=" + person.name(),
                        "period=" + rollover.first().name(), "amount=" + amount.toPlainString()));
            }
        }
        // Kept so the steward can undo it on the same sheet-today.
        rollovers.add(sheet, rollover.fy(), user.trim(), sheets.today(sheet),
                generated.periods().stream().map(Period::name).toList(),
                set.isEmpty() ? Map.of() : Map.of(rollover.first().name(), set));
        audit.record(Audit.line("ROLLOVER", sheet, user, "fy=" + rollover.fy(),
                "periods=" + generated.periods().size()));
    }
}

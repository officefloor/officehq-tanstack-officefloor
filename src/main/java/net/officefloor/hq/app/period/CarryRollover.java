package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.opening.OpeningRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds each person's open figure at the end of the old year's last period (see PeriodCloser#openAt, over the old
 *  year's periods) to their carried figure for the new year's first period (the earliest period whose from-date falls
 *  in it), written as OPENING_SET; a person with nothing open gets no line. Then writes ROLLOVER. */
public class CarryRollover {
    public void carry(@PathVariable("sheetId") String sheetId, @Val RolloverValues rollover,
            @Val GeneratePeriodsValues values, @Val GeneratedPeriods generated, PeriodRepository periods,
            PeriodCloser closer, PersonRepository people, OpeningRepository openings, Audit audit) {
        String sheet = sheetId.trim();
        Period last = rollover.oldPeriods().get(rollover.oldPeriods().size() - 1);
        Map<Long, BigDecimal> open = closer.openAt(sheet, rollover.oldPeriods(), last.to());
        Period first = periods.list(sheet).stream()
                .filter((p) -> !p.from().isBefore(values.from()) && !p.from().isAfter(values.to()))
                .min(Comparator.comparing(Period::from)).orElse(null);
        if (first != null) {
            for (Person person : people.list(sheet)) {
                BigDecimal amount = open.get(person.id());
                if (amount == null || amount.signum() == 0) {
                    continue;
                }
                openings.add(sheet, first.id(), person.id(), amount);
                audit.record("OPENING_SET sheet=" + sheet + " by=" + rollover.by() + " name=" + person.name()
                        + " period=" + first.name() + " amount=" + amount.setScale(2).toPlainString());
            }
        }
        audit.record("ROLLOVER sheet=" + sheet + " by=" + rollover.by() + " fy=" + rollover.fy() + " periods="
                + generated.made());
    }
}

package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a yyyy-mm period for each month first..last, leaving out a month whose name is already a period on the
 *  sheet (without regard to case) or whose days overlap a period on the sheet. */
public class SaveGeneratedPeriods {
    public void save(@PathVariable("sheetId") String sheetId, @Val PeriodGenerateInput input,
            PeriodRepository repository, Out<GeneratedPeriods> generated) {
        String id = sheetId.trim();
        repository.lockSheet(id);
        List<Period> existing = new ArrayList<>(repository.list(id));
        List<Period> made = new ArrayList<>();
        for (YearMonth month = input.first(); !month.isAfter(input.last()); month = month.plusMonths(1)) {
            String name = month.toString();
            LocalDate from = month.atDay(1);
            LocalDate to = month.atEndOfMonth();
            boolean taken = existing.stream()
                    .anyMatch(p -> p.name().trim().equalsIgnoreCase(name) || p.overlaps(from, to));
            if (taken) {
                continue;
            }
            Period period = repository.insert(id, name, from, to);
            existing.add(period);
            made.add(period);
        }
        generated.set(new GeneratedPeriods(made));
    }
}

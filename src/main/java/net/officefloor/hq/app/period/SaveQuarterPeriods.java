package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a <yyyy>-Q<n> period for each calendar quarter starting in first..last, leaving out a quarter whose name is
 *  already a period on the sheet (without regard to case) or whose days overlap a period on the sheet. */
public class SaveQuarterPeriods {
    public void save(@PathVariable("sheetId") String sheetId, @Val PeriodGenerateInput input,
            PeriodRepository repository, Out<GeneratedPeriods> generated) {
        String id = sheetId.trim();
        repository.lockSheet(id);
        List<Period> existing = new ArrayList<>(repository.list(id));
        List<Period> made = new ArrayList<>();
        for (YearMonth start = input.first(); !start.isAfter(input.last()); start = start.plusMonths(3)) {
            String name = start.getYear() + "-Q" + ((start.getMonthValue() - 1) / 3 + 1);
            LocalDate from = start.atDay(1);
            LocalDate to = start.plusMonths(2).atEndOfMonth();
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

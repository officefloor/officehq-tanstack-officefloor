package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.opening.OpeningStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds each person's open figure at the end of the old year's last period (the due of their claims that count,
 *  due in a period of the old year, OPEN or PENDING at that period's end) to their opening balance for the new
 *  year's first period, when this rollover generated it, each recorded as OPENING_SET (a person with nothing open is left out); then records
 *  ROLLOVER sheet=<id> by=<user> fy=FY<old year> periods=<made>. The rollover, the periods it generated and the
 *  opening balances it set are kept so it can be undone (see {@link RolloverStore}). */
public class CarryRollover {
    public void carry(@PathVariable("sheetId") String sheetId, @Val RolloverInput rollover, @Val GenerateResult made,
            PeriodStore periods, OpeningStore openings, PersonStore people, RollupService rollups, RolloverStore rollovers,
            SheetStore sheets, Audit audit) {
        String sheet = sheetId.trim();
        List<LocalDate[]> old = rollover.periods().stream()
                .map(p -> new LocalDate[] { LocalDate.parse(p.from()), LocalDate.parse(p.to()) }).toList();
        LocalDate end = old.stream().map(p -> p[1]).max(LocalDate::compareTo).orElseThrow();
        Map<Long, BigDecimal> open = rollups.carriedByPerson(sheet, end, old);
        LocalDate newFrom = rollover.from().atDay(1);
        LocalDate newNext = rollover.to().plusMonths(1).atDay(1);
        List<PeriodResponse> fresh = periods.list(sheet).stream().filter(p -> {
            LocalDate from = LocalDate.parse(p.from());
            return !from.isBefore(newFrom) && from.isBefore(newNext);
        }).filter(p -> !rollover.before().contains(p.name())).toList();
        PeriodResponse first = fresh.isEmpty() ? null : fresh.get(0);
        long rolloverId = rollovers.add(sheet, rollover.year(), rollover.user(), sheets.today(sheet),
                fresh.stream().map(PeriodResponse::name).toList());
        if (first != null) {
            for (Person person : people.list(sheet)) {
                BigDecimal amount = open.getOrDefault(person.getId(), BigDecimal.ZERO);
                if (amount.signum() <= 0) {
                    continue;
                }
                openings.add(sheet, first.name(), person.getId(), amount);
                rollovers.addOpening(rolloverId, first.name(), person.getId(), amount);
                audit.record("OPENING_SET sheet=" + sheet + " by=" + rollover.user() + " name=" + person.getName()
                        + " period=" + first.name() + " amount=" + amount.setScale(2).toPlainString());
            }
        }
        audit.record("ROLLOVER sheet=" + sheet + " by=" + rollover.user() + " fy=FY" + rollover.year() + " periods="
                + made.generated());
    }
}

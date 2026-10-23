package net.officefloor.hq.app.rollover;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.GeneratedPeriods;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodGenerateInput;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Add each person's open figure at the end of the old year's last period — the dues (as shown now) of their claims
 *  that count then, whose due-by falls in a period of the old year and whose status at that period's end is OPEN or
 *  PENDING — to their carried figure for the new year's first period (OPENING_SET, in person order; none for a figure
 *  of 0.00; none at all when the old year was already rolled over and that rollover still stands), keep the rollover (its periods and openings) so it can be undone, then write ROLLOVER fy=<old FY> periods=<made>. */
public class CarryOpenFigures {
    public void carry(@PathVariable("sheetId") String sheetId, @Val PeriodGenerateInput input, @Val OldYear oldYear,
            @Val GeneratedPeriods generated, PeriodRepository periods, ClaimRepository claims, ClaimLogRepository logs,
            PersonRepository people, RateRepository rates, SettingRepository settings, SheetRepository sheets,
            RolloverRepository rollovers, Audit audit, Out<RolloverResponse> result) {
        String id = sheetId.trim();
        LocalDate newStart = input.first().atDay(1);
        LocalDate newEnd = input.last().atEndOfMonth();
        Period newFirst = periods.list(id).stream()
                .filter(p -> !p.from().isBefore(newStart) && !p.from().isAfter(newEnd)
                        || p.contains(newStart))
                .min(Comparator.comparing(Period::from)).orElse(null);
        List<Long> openings = new ArrayList<>();
        if (newFirst != null && !rollovers.standing(id, oldYear.name())) {
            Period last = oldYear.last();
            var floor = settings.floor(id);
            LateCharge late = LateCharge.of(id, settings, sheets, claims, people);
            Map<Long, ClaimStatus> atEnd = logs.statusesAt(id, last.to());
            Map<Long, BigDecimal> open = new LinkedHashMap<>();
            Map<Long, Person> byId = new LinkedHashMap<>();
            for (Person person : people.list(id)) {
                byId.put(person.id(), person);
                open.put(person.id(), new BigDecimal("0.00"));
            }
            for (Claim c : claims.list(id)) {
                Person person = byId.get(c.personId());
                if (person == null) {
                    continue;
                }
                ClaimStatus status = atEnd.containsKey(c.id()) ? atEnd.get(c.id()) : c.status();
                if ((status != ClaimStatus.OPEN && status != ClaimStatus.PENDING)
                        || !Dues.countsAtEnd(c, status, person.holds(), person.retiredFrom(), late.today())
                        || oldYear.periods().stream().noneMatch(p -> p.contains(c.dueBy()))) {
                    continue;
                }
                BigDecimal due = Dues.shown(c, person.grades(), rates, floor, late);
                if (due != null) {
                    open.merge(person.id(), due, BigDecimal::add);
                }
            }
            open.forEach((personId, amount) -> {
                if (amount.signum() == 0) {
                    return;
                }
                openings.add(periods.addOpening(id, newFirst.name(), personId, amount));
                audit.record("OPENING_SET sheet=" + id + " by=" + input.user() + " name=" + byId.get(personId).name()
                        + " period=" + newFirst.name() + " amount=" + amount.toPlainString());
            });
        }
        rollovers.insert(id, oldYear.name(), input.user(), sheets.today(id),
                generated.periods().stream().map(Period::name).toList(), openings);
        audit.record("ROLLOVER sheet=" + id + " by=" + input.user() + " fy=" + oldYear.name()
                + " periods=" + generated.periods().size());
        result.set(new RolloverResponse(generated.periods().size()));
    }
}

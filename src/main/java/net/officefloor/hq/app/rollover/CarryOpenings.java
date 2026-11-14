package net.officefloor.hq.app.rollover;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.opening.OpeningRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Carries each person's open figure at the end of the old year's last period into the new year's first period: the
 * dues (as they are now) of their claims belonging to a period of the old year, OPEN or PENDING at the last period's
 * end and counted as the period figures count them, added up. Each figure above 0.00 is added to the person's carried
 * figure for the new year's first period, recorded as OPENING_SET, people in the order added.
 */
public class CarryOpenings {
    public void carry(@Val Rollover rollover, @Val Sheet sheet, @RequestParam("user") String user,
            PersonRepository people, ClaimRepository claims, ClaimLogRepository logs, PeriodRepository periods,
            RateRepository rates, SettingRepository settings, OpeningRepository openings, Audit audit) {
        List<Period> sheetPeriods = periods.findAll(sheet.id());
        LocalDate newStart = rollover.newFirst().atDay(1);
        LocalDate newEnd = rollover.newFirst().plusYears(1).atDay(1);
        Period first = sheetPeriods.stream()
                .filter((p) -> !p.from().isBefore(newStart) && p.from().isBefore(newEnd))
                .min(Comparator.comparing(Period::from)).orElse(null);
        if (first == null) {
            return;
        }
        Period last = rollover.last();
        LocalDate today = sheet.today();
        List<Rate> sheetRates = rates.findAll(sheet.id());
        DatedSettings dated = settings.dated(sheet.id());
        List<Claim> sheetClaims = claims.findAll(sheet.id());
        Map<Long, List<ClaimLogLine>> sheetLogs = logs.findAll(sheet.id());
        Rounding rounding = settings.rounding(sheet.id(), today, sheetClaims);
        Map<Long, Person> byId = new LinkedHashMap<>();
        Map<Long, BigDecimal> open = new LinkedHashMap<>();
        people.findAll(sheet.id()).stream().sorted(Comparator.comparing(Person::id)).forEach((person) -> {
            byId.put(person.id(), person);
            open.put(person.id(), BigDecimal.ZERO.setScale(2));
        });
        for (Claim claim : sheetClaims) {
            Person person = byId.get(claim.personId());
            if (person == null) {
                continue;
            }
            Period own = sheetPeriods.stream().filter((p) -> p.contains(claim.dueBy())).findFirst().orElse(null);
            if (own == null || rollover.oldPeriods().stream().noneMatch((p) -> p.name().equals(own.name()))) {
                continue;
            }
            ClaimStatus then = last.statusAtEnd(claim, sheetLogs.getOrDefault(claim.id(), List.of()));
            if (then == null || !then.open() || claim.deferred(today)) {
                continue;
            }
            if (claim.status().open() && !claim.bound() && person.heldOn(claim.date(), today)) {
                continue;
            }
            BigDecimal figure = claim.due(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim),
                    dated.floorOn(claim.date()), dated.lateCharge(today, person), rounding);
            if (figure != null) {
                open.merge(person.id(), figure, BigDecimal::add);
            }
        }
        open.forEach((id, amount) -> {
            if (amount.signum() <= 0) {
                return;
            }
            Person person = byId.get(id);
            openings.add(sheet.id(), first.name(), id, amount);
            audit.record("OPENING_SET sheet=" + sheet.id() + " by=" + user + " name=" + person.name() + " period="
                    + first.name() + " amount=" + amount.setScale(2).toPlainString());
        });
    }
}

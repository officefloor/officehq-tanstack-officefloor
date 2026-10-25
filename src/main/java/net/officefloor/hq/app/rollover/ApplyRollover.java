package net.officefloor.hq.app.rollover;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.MonthlyPeriods;
import net.officefloor.hq.app.period.GeneratedPeriods;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Rolls the fiscal year over: refuses a year none of the sheet's periods belong to (by from-date); else works out
 * each person's open figure at the end of the old year's last period — the dues (with any late charge, as shown now)
 * of their claims that count whose due-by falls in a period of the old year and that were OPEN or PENDING at the end
 * of that last period's last day (by the status log), added up, skipping n/a — then generates the next fiscal year's
 * twelve periods exactly as the fiscal year generator does (PERIOD_ADDED each), adds each person's open figure to
 * their opening balance for the new year's first period (OPENING_SET each with a figure other than zero, in list
 * order) and records
 * ROLLOVER fy=FY&lt;old year&gt; periods=&lt;made&gt;. Keeps what it did as the sheet's last rollover (replacing any
 * earlier one), so the steward who made it can undo it on the same sheet-today. Rolling over again the year the last
 * rollover (not undone) was of carries no opening balance a second time.
 */
public class ApplyRollover {
    public void apply(@Val RolloverRequest request, @Val Sheet sheet, PeriodRepository periods, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log,
            LastRolloverRepository lastRollovers, @RequestHeader(name = "X-User", required = false) String user, Audit audit, Out<RolledOver> rolledOut)
            throws InvalidRolloverException {
        String by = AuditUser.decode(user);
        int year = Integer.parseInt(request.year());
        YearMonth oldFirst = YearMonth.of(year, sheet.getFiscalStartMonth());
        LocalDate oldStart = oldFirst.atDay(1);
        LocalDate newStart = oldFirst.plusMonths(12).atDay(1);
        List<Period> oldYear = periods.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()).stream()
                .filter(p -> !p.getFromDate().isBefore(oldStart) && p.getFromDate().isBefore(newStart)).toList();
        if (oldYear.isEmpty()) {
            throw new InvalidRolloverException("The sheet has no periods in fiscal year " + request.year() + ".");
        }
        Period last = oldYear.stream().max(Comparator.comparing(Period::getToDate)).get();

        List<Person> listed = people.findListed(sheet.getId());
        Map<Long, Person> byId = new HashMap<>();
        listed.forEach(p -> byId.put(p.getId(), p));
        var sheetRates = rates.findInChain(sheet.getId());
        Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(sheet.getId());
        Map<Long, ClaimStatus> then = log.statusesOn(sheet.getId(), last.getToDate());
        Map<Long, BigDecimal> open = new HashMap<>();
        listed.forEach(p -> open.put(p.getId(), BigDecimal.ZERO.setScale(2)));
        claims.findBySheetIdOrderByIdDesc(sheet.getId()).stream()
                .filter(c -> byId.containsKey(c.getPersonId()) && oldYear.stream().anyMatch(q -> q.contains(c.getDueBy())))
                .filter(c -> {
                    ClaimStatus status = then.get(c.getId());
                    return (status == ClaimStatus.OPEN || status == ClaimStatus.PENDING)
                            && ClaimFigures.countsAsOf(c, status, byId.get(c.getPersonId()), sheet.getToday());
                })
                .forEach(c -> {
                    BigDecimal due = new ClaimFigures(sheetRates, new PersonGrades(byId.get(c.getPersonId()).getGrade(),
                            dated.getOrDefault(c.getPersonId(), List.of())), sheet).due(c);
                    if (due != null) {
                        open.merge(c.getPersonId(), due, BigDecimal::add);
                    }
                });

        YearMonth newFirst = oldFirst.plusMonths(12);
        GeneratedPeriods made = MonthlyPeriods.generate(sheet, newFirst, newFirst.plusMonths(11), periods);
        List<String> records = new ArrayList<>();
        List<LastRollover.Opening> openings = new ArrayList<>();
        for (Period period : made.periods()) {
            records.add("PERIOD_ADDED sheet=" + sheet.getId() + " by=" + by + " period=" + period.getName()
                    + " from=" + period.getFromDate() + " to=" + period.getToDate());
        }
        LocalDate newEnd = newFirst.plusMonths(12).atDay(1);
        Period first = periods.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()).stream()
                .filter(p -> !p.getFromDate().isBefore(newStart) && p.getFromDate().isBefore(newEnd))
                .findFirst().orElse(null);
        LastRollover earlier = lastRollovers.findById(sheet.getId()).orElse(null);
        boolean carried = earlier != null && earlier.getFiscalYear().equals(request.year());
        if (first != null && !carried) {
            for (Person person : listed) {
                BigDecimal amount = open.get(person.getId()).setScale(2);
                if (amount.signum() == 0) {
                    continue;
                }
                first.addOpening(person.getId(), amount);
                openings.add(new LastRollover.Opening(first.getId(), person.getId(), amount));
                records.add("OPENING_SET sheet=" + sheet.getId() + " by=" + by + " name=" + person.getName()
                        + " period=" + first.getName() + " amount=" + amount.toPlainString());
            }
            periods.saveAndFlush(first);
        }
        if (earlier != null) {
            lastRollovers.delete(earlier);
            lastRollovers.flush();
        }
        lastRollovers.saveAndFlush(new LastRollover(sheet.getId(), by, sheet.getToday(), request.year(),
                made.periods().stream().map(Period::getId).toList(), openings));
        records.add("ROLLOVER sheet=" + sheet.getId() + " by=" + by + " fy=FY" + request.year()
                + " periods=" + made.periods().size());
        records.forEach(audit::record);
        rolledOut.set(new RolledOver(made.periods().size()));
    }
}

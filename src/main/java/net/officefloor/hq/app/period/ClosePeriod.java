package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Closes the period, fixing each person's due-total for it as it is now (and each pool member's cut for it as it is now) — the dues (with any late charge, as shown now) of the claims whose due-by falls in it that counted by the status they had at the end of its last day (not a person's
 *  unbound OPEN ones dated within a hold of theirs), added up, skipping n/a, 0.00 for one with none — and its own, their sum. Each person's carried figure for it is fixed as it is now too. A soft-closed period can be closed, fixing them afresh. Refuses a period already closed, and a close by a role the sheet's permission matrix does not
 *  allow to (by the standing rule, clerk and steward may). */
public class ClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, ClaimRepository claims, PersonRepository people,
            RateRepository rates, SheetRepository sheets, PersonGradeRepository grades, ClaimLogRepository log,
            RoleRepository roles, AsOf asOf, @RequestHeader(name = "X-User", required = false) String user) throws InvalidPeriodException {
        Sheet sheet = sheets.findById(period.getSheetId()).orElseGet(() -> new Sheet(period.getSheetId()));
        String role = roles.roleOf(period.getSheetId(), AuditUser.decode(user));
        if (!sheet.allows("close", role)) {
            throw new InvalidPeriodException("A " + role + " cannot close a period.");
        }
        if (period.isClosed()) {
            throw new InvalidPeriodException("The period is already closed.");
        }
        fix(period, repository, sheet, claims, people, rates, grades, log, asOf);
        repository.saveAndFlush(period);
    }

    /** Closes the period on the sheet, fixing each person's due-total and carried figure for it, and each pool
     *  member's cut, as they are now (see above). */
    static void fix(Period period, PeriodRepository repository, Sheet sheet, ClaimRepository claims, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log, AsOf asOf) {
        period.close(totals(period, sheet, claims, people, rates, grades, log),
                carried(period, repository, sheet, claims, people, rates, grades, log));
        period.fixPoolCuts(PoolCuts.live(sheet, asOf));
    }

    /** Each person's carried figure for the period as it is now, by person id: the dues (with any late charge, as shown
     *  now) of their claims that count whose due-by falls in a period ending before this one starts and that were still
     *  OPEN or PENDING at the end of this one's last day (by the status log), added up, skipping n/a, plus their opening balance for it; 0.00 for none. */
    static Map<Long, BigDecimal> carried(Period period, PeriodRepository repository, Sheet sheet, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log) {
        Map<Long, Person> byId = people.findListed(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<Period> earlier = repository.findBySheetIdOrderByFromDateAscIdAsc(period.getSheetId()).stream()
                .filter(q -> q.getToDate().isBefore(period.getFromDate())).toList();
        var sheetRates = rates.findInChain(period.getSheetId());
        Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(period.getSheetId());
        Map<Long, ClaimStatus> then = log.statusesOn(period.getSheetId(), period.getToDate());
        Map<Long, BigDecimal> carried = new HashMap<>();
        byId.keySet().forEach(id -> carried.put(id, BigDecimal.ZERO.setScale(2)));
        claims.findBySheetIdOrderByIdDesc(period.getSheetId()).stream()
                .filter(c -> byId.containsKey(c.getPersonId()) && earlier.stream().anyMatch(q -> q.contains(c.getDueBy())))
                .filter(c -> {
                    ClaimStatus status = then.get(c.getId());
                    return (status == ClaimStatus.OPEN || status == ClaimStatus.PENDING)
                            && ClaimFigures.countsAsOf(c, status, byId.get(c.getPersonId()), sheet.getToday());
                })
                .forEach(c -> {
                    BigDecimal due = new ClaimFigures(sheetRates, new PersonGrades(byId.get(c.getPersonId()).getGrade(),
                            dated.getOrDefault(c.getPersonId(), List.of())), sheet).due(c);
                    if (due != null) {
                        carried.merge(c.getPersonId(), due, BigDecimal::add);
                    }
                });
        carried.replaceAll((id, t) -> t.add(period.openingOf(id)).setScale(2));
        return carried;
    }

    /** Each person's due-total for the period as it is now (see above), by person id. */
    static Map<Long, BigDecimal> totals(Period period, Sheet sheet, ClaimRepository claims, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log) {
        Map<Long, Person> byId = people.findListed(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        var sheetRates = rates.findInChain(period.getSheetId());
        Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(period.getSheetId());
        Map<Long, ClaimStatus> then = log.statusesOn(period.getSheetId(), period.getToDate());
        Map<Long, BigDecimal> totals = new HashMap<>();
        byId.keySet().forEach(id -> totals.put(id, BigDecimal.ZERO.setScale(2)));
        claims.findBySheetIdOrderByIdDesc(period.getSheetId()).stream()
                .filter(c -> byId.containsKey(c.getPersonId()) && ClaimFigures.countsAsOf(c, then.get(c.getId()), byId.get(c.getPersonId()), sheet.getToday())
                        && period.contains(c.getDueBy()))
                .forEach(c -> {
                    BigDecimal due = new ClaimFigures(sheetRates, new PersonGrades(byId.get(c.getPersonId()).getGrade(),
                            dated.getOrDefault(c.getPersonId(), List.of())), sheet).due(c);
                    if (due != null) {
                        totals.merge(c.getPersonId(), due, BigDecimal::add);
                    }
                });
        totals.replaceAll((id, t) -> t.setScale(2));
        return totals;
    }
}

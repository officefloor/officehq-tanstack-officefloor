package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing each person's due-total for it as it is now — the dues (with any late charge) of the counted claims dated in it (not a person's
 *  unbound OPEN ones dated within a hold of theirs), added up, skipping n/a, 0.00 for one with none — and its own, their sum. Refuses a period already closed. */
public class ClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, ClaimRepository claims, PersonRepository people,
            RateRepository rates, SheetRepository sheets, PersonGradeRepository grades) throws InvalidPeriodException {
        if (period.isClosed()) {
            throw new InvalidPeriodException("The period is already closed.");
        }
        Sheet sheet = sheets.findById(period.getSheetId()).orElseGet(() -> new Sheet(period.getSheetId()));
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        var sheetRates = rates.findBySheetIdOrderByGradeAscFromDateAscIdAsc(period.getSheetId());
        Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(period.getSheetId());
        Map<Long, BigDecimal> totals = new HashMap<>();
        byId.keySet().forEach(id -> totals.put(id, BigDecimal.ZERO.setScale(2)));
        claims.findBySheetIdOrderByIdDesc(period.getSheetId()).stream()
                .filter(c -> byId.containsKey(c.getPersonId()) && ClaimFigures.countsLive(c, byId.get(c.getPersonId()))
                        && period.contains(c.getDate()))
                .forEach(c -> {
                    BigDecimal due = new ClaimFigures(sheetRates, new PersonGrades(byId.get(c.getPersonId()).getGrade(),
                            dated.getOrDefault(c.getPersonId(), List.of())), sheet).due(c);
                    if (due != null) {
                        totals.merge(c.getPersonId(), due, BigDecimal::add);
                    }
                });
        totals.replaceAll((id, t) -> t.setScale(2));
        period.close(totals);
        repository.saveAndFlush(period);
    }
}

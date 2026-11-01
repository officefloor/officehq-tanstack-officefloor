package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Carries each person's open figure at the end of the old year's last period — the figures of their
 *  counted claims belonging, by due-by, to a period of the old year that were OPEN or PENDING at the
 *  end of that last period's last day, by their status log — into their opening balance for the new
 *  year's first period, audited OPENING_SET (a person with nothing open gets no line; a rollover that
 *  made no periods, the year being rolled over already, carries nothing again); then audits
 *  ROLLOVER with how many periods the generate made. The rollover is kept as the sheet's last, so it
 *  can be undone. */
public class CarryRollover {
    public void carry(@PathVariable("sheetId") String sheetId, @Val RolloverYear old,
            @Val GeneratePeriodsResult generated, Periods periods, Claims claims, People people, Figures figures,
            ClaimLog log, Settings settings, Sheets sheets, Rollovers rollovers, Audit audit, Out<RolloverResult> result) {
        Period last = old.periods().stream().max(Comparator.comparing(Period::to)).orElseThrow();
        int startMonth = settings.fiscalStartMonth(sheetId);
        Map<String, Map<Long, BigDecimal>> set = new LinkedHashMap<>();
        Period first = periods.onSheet(sheetId).stream()
                .filter(p -> ValidateRollover.fiscalYear(p, startMonth) == old.year() + 1)
                .findFirst().orElse(null);
        if (first != null && generated.generated() > 0) {
            Map<Long, ClaimStatus> atEnd = log.statusesAt(sheetId, last.to());
            List<Person> persons = people.onSheet(sheetId);
            Map<Long, BigDecimal> open = new LinkedHashMap<>();
            for (Person person : persons) open.put(person.getId(), BigDecimal.ZERO.setScale(2));
            Map<Long, Person> personOf = new LinkedHashMap<>();
            for (Person person : persons) personOf.put(person.getId(), person);
            for (Claim claim : claims.onSheet(sheetId)) {
                if (old.periods().stream().noneMatch(p -> p.contains(claim.getDueBy()))) continue;
                Person person = personOf.get(claim.getPersonId());
                ClaimStatus status = atEnd.containsKey(claim.getId()) ? atEnd.get(claim.getId()) : claim.getStatus();
                if (person == null || status == null || !status.isOpen() || !person.counts(claim, status)) continue;
                BigDecimal figure = figures.due(claim, person.gradeOn(claim.getDate()));
                if (figure != null) open.merge(person.getId(), figure, BigDecimal::add);
            }
            for (Person person : persons) {
                BigDecimal amount = open.get(person.getId());
                if (amount.signum() == 0) continue;
                periods.addOpening(first, person.getId(), amount);
                set.computeIfAbsent(first.name(), k -> new LinkedHashMap<>()).put(person.getId(), amount);
                audit.record("OPENING_SET sheet=" + sheetId + " by=" + old.user() + " name=" + person.getName()
                        + " period=" + first.name() + " amount=" + amount.setScale(2).toPlainString());
            }
        }
        rollovers.save(new Rollovers.Made(sheetId, old.year(), old.user(), sheets.today(sheetId),
                generated.names(), set));
        audit.record("ROLLOVER sheet=" + sheetId + " by=" + old.user() + " fy=FY" + old.year()
                + " periods=" + generated.generated());
        result.set(new RolloverResult(generated.generated()));
    }
}

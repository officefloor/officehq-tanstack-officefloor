package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** For every plan on the sheet, in the order they were added, adds a DRAFT claim of the plan's amount,
 *  dated that day with an empty note, for every plan date (the start, then every N days) up to the end
 *  and the sheet's today that the plan has made none for yet. Each is checked as
 *  {@link net.officefloor.hq.app.claim.AddClaim} checks a claim (its person not retired before its date,
 *  its due-by outside a closed period): a date that is refused is skipped, to be tried again next time.
 *  Each claim made is logged and recorded as CLAIM_ADDED. */
public class GeneratePlanClaims {
    public void generate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, PlanRepository plans,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims, PeriodRepository periods,
            SettingRepository settings, HolidayRepository holidays, ClaimLogRepository log, Audit audit,
            Out<GenerateResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        LocalDate today = sheets.today(id);
        int made = 0;
        for (Plan plan : plans.list(id)) {
            Person person = people.find(id, plan.personId());
            if (person == null) continue;
            Set<LocalDate> done = plans.made(plan.id());
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate date = plan.start(); !date.isAfter(last); date = date.plusDays(plan.every())) {
                if (done.contains(date) || person.retiredBefore(date)) continue;
                // The due-by is fixed now, as for a claim added on the form.
                LocalDate dueBy = holidays.dueBy(id, date, settings.terms(id, person, date.toString()));
                try {
                    periods.requireOpen(id, dueBy.toString());
                } catch (PeriodClosedException e) {
                    continue;
                }
                Claim claim = claims.add(id, person.getId(), plan.amount(), date, "", null, dueBy, user);
                if (periods.softClosedOn(id, dueBy.toString())) claims.markLateEntry(claim);
                plans.recordMade(plan.id(), date, claim.getId());
                log.record(claim, user);
                audit.record("CLAIM_ADDED sheet=" + id + " by=" + user + " person=" + person.getName()
                        + " amount=" + claim.getAmount() + " date=" + claim.getDate() + claim.getAuditRef());
                if (claim.isLateEntry()) {
                    audit.record("LATE_ENTRY sheet=" + id + " by=" + user + " person=" + person.getName()
                            + " amount=" + claim.getAmount() + " date=" + claim.getDate());
                }
                made++;
            }
        }
        result.set(new GenerateResult(made));
    }
}

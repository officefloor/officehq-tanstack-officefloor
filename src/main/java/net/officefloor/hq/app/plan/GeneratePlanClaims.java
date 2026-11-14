package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.CheckClaimInputNotRetired;
import net.officefloor.hq.app.claim.CheckClaimInputPeriodOpen;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDueNow;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.LateEntries;
import net.officefloor.hq.app.claim.NewDueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * For every plan on the sheet, in the order they were added, adds a DRAFT claim of the plan's amount, dated that day
 * with an empty note, for each plan date (the start, then every N days; or
 * that day in every month from the start's month, on or after the start) up to its end and the sheet's today that it
 * has made none for yet, leaving out the dates a pause skips. Each is checked and added as the claim form adds a claim (no claim after the person's
 * retirement date, none due by inside a closed period; due-by fixed, DRAFT logged, CLAIM_ADDED, LATE_ENTRY when its
 * period is soft-closed); a date the form refuses is skipped and tried again next time. A viewer changes nothing: a
 * viewer's generate that would make a claim is refused (400) before it makes any, one with nothing to make makes none.
 * A plan naming a descendant sheet makes its claims there instead, for the person of its person's name on that sheet
 * (none when there is none), by that sheet's rules and in its audit, followed by that sheet's rollup lines; the plan's
 * dates still run to this sheet's today and it still counts what it made.
 */
public class GeneratePlanClaims {
    public void generate(@Val Sheet sheet, @RequestParam("user") String user, PlanRepository plans,
            SheetRepository sheets, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods,
            PersonRepository people, RateRepository rates, ClaimRepository claims, ClaimLogRepository logs, Audit audit,
            LateEntries lateEntries, RollupService rollups, Out<GenerateResult> result) throws ViewerRefusedException {
        LocalDate today = sheets.today(sheet.id());
        List<Plan> all = plans.findAll(sheet.id());
        Map<String, Rollup> before = new LinkedHashMap<>();
        for (Plan plan : all) {
            if (plan.targetSheet() != null && !before.containsKey(plan.targetSheet())) {
                before.put(plan.targetSheet(), rollups.compute(plan.targetSheet()));
            }
        }
        int made = 0;
        for (Plan plan : all) {
            Person person = people.find(sheet.id(), plan.personId());
            if (person == null) {
                continue;
            }
            String on = sheet.id();
            if (plan.targetSheet() != null) {
                on = plan.targetSheet();
                person = people.findByName(on, person.name());
                if (person == null) {
                    continue;
                }
            }
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            Set<LocalDate> done = new HashSet<>(plans.datesMade(plan.id()));
            for (LocalDate date : plan.activeDates(last)) {
                if (done.contains(date)) {
                    continue;
                }
                ClaimInput input = new ClaimInput(plan.amount(), date, "", null, null, null);
                try {
                    new CheckClaimInputNotRetired().check(input, person, on, sheets);
                    new CheckClaimInputPeriodOpen().check(input, person, on, sheets, settings, holidays,
                            periods);
                } catch (InvalidClaimException e) {
                    continue;
                }
                if (settings.find(sheet.id()).roleOf(user) == Role.VIEWER) {
                    throw new ViewerRefusedException("A viewer cannot change the sheet.");
                }
                LocalDate dueBy = NewDueBy.of(date, person, sheets, settings, holidays);
                Claim claim = claims.save(new Claim(null, on, person.id(), plan.amount(), date, "",
                        ClaimStatus.DRAFT, null, null, null, false, null, null, dueBy));
                BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
                logs.append(claim.sheetId(), new ClaimLogLine(claim.id(), claim.status(), user, sheets.today(on),
                        due == null ? "n/a" : due.setScale(2).toPlainString()));
                audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + user + " person=" + person.name()
                        + " amount=" + claim.amount().setScale(2).toPlainString() + " date=" + claim.date());
                lateEntries.mark(claim, person, user);
                plans.markMade(plan, date, claim);
                made++;
            }
        }
        for (Map.Entry<String, Rollup> was : before.entrySet()) {
            RollupLines.record(was.getValue(), rollups.compute(was.getKey()), was.getKey(), user, audit);
        }
        result.set(new GenerateResult(made));
    }
}

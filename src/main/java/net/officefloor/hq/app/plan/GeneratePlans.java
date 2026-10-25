package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * For every plan on the sheet, in the order they were added, makes a DRAFT claim with the plan's amount for each plan
 * date (the start, then every N days) up to its end and the sheet's today that has none yet and is not skipped by a
 * pause (on or after the day the plan was paused and before the day it was resumed), dated that day with an
 * empty note, by the claim form's rules: a date the form refuses is skipped, and tried again next time. Each claim is
 * recorded and logged as the claim form records and logs it. A viewer, who changes nothing, makes no claim and gets a
 * count of 0.
 */
public class GeneratePlans {
    public void generate(@Val Sheet sheet, PlanRepository plans, PlanClaimRepository made, PlanPauseRepository pauses,
            PersonRepository people,
            ClaimRepository claims, PeriodRepository periods, PersonGradeRepository grades, RateRepository rates,
            ClaimLogRepository log, HolidayRepository holidays, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<GeneratedPlans> madeOut) {
        String by = AuditUser.decode(user);
        int count = 0;
        if (Role.VIEWER.equals(roles.roleOf(sheet.getId(), by))) {
            madeOut.set(new GeneratedPlans(count));
            return;
        }
        for (Plan plan : plans.findBySheetIdOrderByIdAsc(sheet.getId())) {
            Person person = people.findByIdAndSheetId(plan.getPersonId(), sheet.getId()).orElse(null);
            if (person == null) {
                continue;
            }
            Set<LocalDate> done = new HashSet<>();
            made.findByPlanId(plan.getId()).forEach(m -> done.add(m.getOnDate()));
            for (LocalDate date : plan.datesUpTo(sheet.getToday())) {
                if (done.contains(date) || pauses.skips(plan.getId(), date)) {
                    continue;
                }
                Claim claim;
                try {
                    claim = ClaimRules.make(new ClaimInput(plan.getAmount(), date, "", null, null, null), sheet,
                            person, claims, periods, grades, holidays);
                } catch (InvalidClaimException e) {
                    continue;
                }
                claim = claims.saveAndFlush(claim);
                made.saveAndFlush(new PlanClaim(plan.getId(), date, claim.getId()));
                ClaimRules.auditAdded(claim, person.getName(), by, audit);
                ClaimRules.log(claim, sheet, log, people, rates, grades, by);
                count++;
            }
        }
        madeOut.set(new GeneratedPlans(count));
    }
}

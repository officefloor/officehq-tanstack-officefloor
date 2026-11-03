package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** For every plan on the sheet, in the order they were added, make a DRAFT claim with the plan's amount for each
 *  plan date (the start, then every N days) up to the end and the sheet's today that has none yet, dated that day
 *  with an empty note, written as CLAIM_ADDED. Each is checked by the claim form's rules: a date the form refuses
 *  is skipped (nothing written) and tried again next time. Answers how many were made. */
public class GeneratePlanClaims {
    public void generate(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PlanRepository plans, ClaimRepository claims, SheetRepository sheets, PeriodRepository periods,
            PersonRepository people, Audit audit, Rollup rollup, Out<GenerateResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!plans.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        LocalDate today = sheets.today(id);
        int made = 0;
        for (Plan plan : plans.list(id)) {
            String name = plans.personName(id, plan.personId());
            if (name == null) {
                continue;
            }
            Set<LocalDate> done = plans.madeOn(plan.id());
            LocalDate end = LocalDate.parse(plan.end());
            LocalDate last = today != null && today.isBefore(end) ? today : end;
            BigDecimal amount = new BigDecimal(plan.amount());
            for (LocalDate on = LocalDate.parse(plan.start()); !on.isAfter(last); on = on.plusDays(plan.every())) {
                if (done.contains(on)) {
                    continue;
                }
                try {
                    PeriodGuard.open(periods, id, claims.dueBy(id, plan.personId(), on));
                    people.claimAllowed(id, plan.personId(), on);
                } catch (BadRequestException refused) {
                    continue;
                }
                Rollup.Snapshot before = rollup.take(id);
                Claim added = claims.add(id, plan.personId(), amount, on, "", null);
                claims.log(id, added.id(), by);
                plans.made(id, plan.id(), on, added.id());
                audit.record("CLAIM_ADDED sheet=" + id + " by=" + by + " person=" + name
                        + " amount=" + added.amount() + " date=" + added.date());
                if (added.lateEntry()) {
                    audit.record("LATE_ENTRY sheet=" + id + " by=" + by + " person=" + name
                            + " amount=" + added.amount() + " date=" + added.date());
                }
                rollup.record(id, by, before);
                made++;
            }
        }
        result.set(new GenerateResult(made));
    }
}

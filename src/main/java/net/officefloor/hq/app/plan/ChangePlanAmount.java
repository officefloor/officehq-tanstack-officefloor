package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.AuditClaimEdited;
import net.officefloor.hq.app.claim.CheckClaimDraft;
import net.officefloor.hq.app.claim.CheckClaimLinesAmount;
import net.officefloor.hq.app.claim.CheckClaimNotLocked;
import net.officefloor.hq.app.claim.CheckClaimPeriodOpen;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLockRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Changes the plan's amount on the sheet's today (PLAN_CHANGED): the claims it makes from then on are of the new
 * amount. The claims it already made that are still DRAFT are edited to the new amount as the claim's editor would
 * (last editor marked, CLAIM_EDITED, then the rollup lines they moved); one the editor would refuse (locked by another
 * user, due by inside a closed period, its lines making another amount), or in any other status, keeps its amount.
 * Refused (400) for an amount the plan form would refuse.
 */
public class ChangePlanAmount {
    public void change(@Val Plan plan, @Val PlanAmountRequest request, @RequestParam("user") String user,
            PlanRepository plans, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            ClaimLockRepository locks, ClaimLineRepository claimLines, PeriodRepository periods,
            RollupService rollups, Audit audit, Out<Plans> saved) throws InvalidPlanException {
        BigDecimal amount;
        try {
            amount = ClaimForm.check(new ClaimRequest(request.amount(), "", "", "", "", "")).amount();
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        String on = plan.targetSheet() != null ? plan.targetSheet() : plan.sheetId();
        Rollup before = rollups.compute(on);
        plans.changeAmount(plan, sheets.today(plan.sheetId()), amount);
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + user + " name=" + person.name() + " start="
                + plan.start() + " amount=" + amount.setScale(2).toPlainString());
        ClaimInput input = new ClaimInput(amount, null, "", null, null, null);
        for (long id : plans.claimsMade(plan.id())) {
            Claim claim = claims.find(on, id);
            if (claim == null || claim.amount().compareTo(amount) == 0) {
                continue;
            }
            try {
                new CheckClaimDraft().check(claim);
                new CheckClaimNotLocked().check(claim, user, locks);
                new CheckClaimPeriodOpen().check(claim, periods);
                new CheckClaimLinesAmount().check(claim, input, claimLines);
            } catch (InvalidClaimException e) {
                continue;
            }
            Claim edited = claims.save(new Claim(claim.id(), claim.sheetId(), claim.personId(), amount, claim.date(),
                    claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(),
                    claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(),
                    claim.splitFrom(), claim.added(), claim.waived(), claim.held()));
            claims.markEdited(edited.sheetId(), edited.id(), user);
            new AuditClaimEdited().audit(edited, people, user, audit);
        }
        RollupLines.record(before, rollups.compute(on), on, user, audit);
        saved.set(new Plans(java.util.List.of(plans.find(plan.sheetId(), plan.id()))));
    }
}

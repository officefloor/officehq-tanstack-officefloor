package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimEdit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLockedException;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStatusException;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Changes the plan's amount from the sheet's today (PLAN_CHANGED): the claims it makes from then on are of
 *  the new amount. The claims it has made that are still DRAFT take the new amount, each edited as the claim
 *  form would edit it (CLAIM_EDITED); one the form would refuse, and a claim in any other status, keeps its own. */
public class ChangePlanAmount {
    public void change(@RequestParam(name = "by", required = false) String by, @Val ValidPlanAmount request,
            @Val Plan plan, @Val Person person, PlanRepository plans, SheetRepository sheets, Audit audit,
            ClaimRepository claims, PeriodRepository periods, PersonRepository people, ClaimLineRepository lines) {
        String user = by == null ? "" : by;
        BigDecimal amount = request.amount();
        plans.changeAmount(plan.id(), sheets.today(plan.sheetId()), amount);
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + user
                + " name=" + person.getName() + " start=" + plan.start() + " amount=" + amount.toPlainString());
        String on = plan.targetSheet() == null ? plan.sheetId() : plan.targetSheet();
        for (long claimId : plans.madeClaims(plan.id())) {
            Claim claim = claims.find(on, claimId);
            if (claim == null || !ClaimStatus.DRAFT.name().equals(claim.getStatus())) continue;
            ValidClaim edit = new ValidClaim(amount, LocalDate.parse(claim.getDate()), claim.getNote(),
                    claim.getCorrects(), claim.getRef(), claim.getTags());
            try {
                ClaimEdit.apply(user, edit, claim, sheets, periods, people, claims, lines);
            } catch (ClaimStatusException | PeriodClosedException | InvalidClaimException | ClaimLockedException refused) {
                continue;
            }
            claims.update(claim);
            Person owner = people.find(claim.getSheetId(), claim.getPersonId());
            audit.record("CLAIM_EDITED sheet=" + claim.getSheetId() + " by=" + user
                    + " person=" + (owner == null ? "" : owner.getName())
                    + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                    + claim.getAuditNumber());
        }
    }
}

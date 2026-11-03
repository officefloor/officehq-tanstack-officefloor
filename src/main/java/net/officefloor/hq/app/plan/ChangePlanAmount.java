package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAmountEdit;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Change a plan's amount (more than zero, up to two decimals) from the sheet's today, written as PLAN_CHANGED. The
 *  claims it generates from then on take the new amount; those already generated that are still DRAFT take it too,
 *  in the order of their dates, each as the claim form would edit its amount (CLAIM_EDITED); claims in any other
 *  status keep theirs. One change: if any draft's edit is refused, the whole change is refused — the store is rolled
 *  back and no audit record is written. */
public class ChangePlanAmount {

    /** Carries a refusal out of the transaction, which it rolls back. */
    private static final class Refused extends RuntimeException {
        final BadRequestException refusal;

        Refused(BadRequestException refusal) {
            super(refusal.getMessage(), null, false, false);
            this.refusal = refusal;
        }
    }

    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            @RequestBody PlanAmountRequest request, ServerHttpConnection connection, PlanRepository repository,
            SheetRepository sheets, ClaimRepository claims, PeriodRepository periods, Rollup rollup, Audit audit,
            PlatformTransactionManager transactions) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        BigDecimal value = ClaimInput.signedAmount(request == null || request.amount() == null ? "" : request.amount().trim());
        if (value.signum() <= 0) {
            throw new BadRequestException("amount must be more than 0");
        }
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        try {
            audit.held(() -> transaction.execute(status -> {
                try {
                    apply(id, planId, value, by, repository, sheets, claims, periods, rollup, audit);
                    return null;
                } catch (BadRequestException e) {
                    throw new Refused(e);
                }
            }));
        } catch (Refused e) {
            throw e.refusal;
        }
    }

    private static void apply(String id, String planId, BigDecimal value, String by, PlanRepository repository,
            SheetRepository sheets, ClaimRepository claims, PeriodRepository periods, Rollup rollup, Audit audit)
            throws BadRequestException {
        Plan plan = repository.lockedPlan(id, planId);
        String amount = repository.changeAmount(id, plan.id(), sheets.today(id), value);
        audit.record("PLAN_CHANGED sheet=" + id + " by=" + by + " name=" + repository.personName(id, plan.personId())
                + " start=" + plan.start() + " amount=" + amount);
        // the plan's claims live on the sheet it names, if any
        String target = plan.sheet() == null ? id : plan.sheet();
        for (long cid : repository.claimsMade(plan.id())) {
            Claim found = claims.find(target, cid);
            if (found == null || !found.status().equals("DRAFT")) {
                continue;
            }
            ClaimAmountEdit.apply(target, cid, value, by, claims, periods, audit, rollup);
        }
    }
}

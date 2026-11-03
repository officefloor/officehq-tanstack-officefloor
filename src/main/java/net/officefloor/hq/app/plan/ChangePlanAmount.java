package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Change a plan's amount (more than zero, up to two decimals) from the sheet's today, written as PLAN_CHANGED. The
 *  claims it generates from then on take the new amount; those already generated keep theirs. */
public class ChangePlanAmount {
    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            @RequestBody PlanAmountRequest request, ServerHttpConnection connection, PlanRepository repository,
            SheetRepository sheets, Audit audit) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        BigDecimal value = ClaimInput.signedAmount(request == null || request.amount() == null ? "" : request.amount().trim());
        if (value.signum() <= 0) {
            throw new BadRequestException("amount must be more than 0");
        }
        Plan plan = repository.lockedPlan(id, planId);
        String amount = repository.changeAmount(id, plan.id(), sheets.today(id), value);
        audit.record("PLAN_CHANGED sheet=" + id + " by=" + by + " name=" + repository.personName(id, plan.personId())
                + " start=" + plan.start() + " amount=" + amount);
    }
}

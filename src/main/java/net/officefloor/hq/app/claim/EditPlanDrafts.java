package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.CheckClaimPeriod;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanAmount;
import net.officefloor.hq.app.plan.PlanRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Gives the plan's new amount to the claims it generated that are still DRAFT, in the order it made them,
 * each as the claim form edits it: saved with the signed-in user as its last editor and audited as
 * CLAIM_EDITED. A draft the editor would refuse (its editor open by another user, its due-by inside a
 * closed period, its lines' total another amount) keeps its amount, as does a claim in any other status
 * and a draft that already has the new amount. Drafts the plan made on a descendant sheet are edited
 * there, in that sheet's audit, followed by that sheet's rollup lines.
 */
public class EditPlanDrafts {
    public void edit(@Val PlanAmount input, @Val Sheet sheet, @Val Plan plan, PlanRepository plans,
            ClaimRepository claims, ClaimLineRepository lines, PeriodRepository periods, PersonRepository people,
            RateRepository rates, SettingsRepository settings, SheetRepository sheets, HttpServletRequest request,
            Audit audit) {
        String user = Audit.user(request);
        BigDecimal amount = input.amount();
        Sheet on = plan.target() == null ? sheet : sheets.find(plan.target());
        if (on == null) return;
        Rollup before = on == sheet ? null
                : Rollup.of(on.id(), on.today(), people, claims, rates, settings.find(on.id()));
        boolean edited = false;
        for (long id : plans.madeClaims(plan.id())) {
            Claim claim = claims.find(on.id(), id);
            if (claim == null || claim.getStatus() != ClaimStatus.DRAFT) continue;
            if (claim.getAmount().compareTo(amount) == 0) continue;
            if (claims.lockedByOther(claim, user, on.today())) continue;
            BigDecimal total = lines.total(claim);
            if (total != null && total.compareTo(amount) != 0) continue;
            try {
                new CheckClaimPeriod().check(on, claim, periods);
            } catch (ClosedPeriodException e) {
                continue;
            }
            claim.setAmount(amount);
            claim.setEditedBy(user);
            claims.save(claim);
            audit.record(ClaimAudit.line("CLAIM_EDITED", on, user, people.find(on.id(), claim.getPersonId()), claim));
            edited = true;
        }
        // A descendant written to: the figures the edits moved there, in its own audit.
        if (before != null && edited) {
            Rollup after = Rollup.of(on.id(), sheets.today(on.id()), people, claims, rates, settings.find(on.id()));
            after.changesSince(before, on.id(), user).forEach(audit::record);
        }
    }
}

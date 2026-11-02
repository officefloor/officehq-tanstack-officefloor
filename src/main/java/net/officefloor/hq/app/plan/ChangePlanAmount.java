package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Changes the plan's amount from the sheet's today: 0.00 or more with up to two decimals, as a plan is added with. The
 * claims generated from then on take the new amount, and so do the ones it generated that are still DRAFT, each edited
 * as the claim form's SaveClaim edits it (CLAIM_EDITED, by plan date); one the form would refuse (due inside a closed
 * period, with lines of another total, dated after its person's retirement, or held by another user's editor) keeps its amount, as do
 * claims in any other status.
 */
public class ChangePlanAmount {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void change(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Plan plan,
            @RequestBody PlanAmountRequest request, Plans plans, People people, SheetClock clock, Claims claims,
            Periods periods, Rollup rollup, Audit audit)
            throws InvalidPlanException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidPlanException("The amount must be 0.00 or more with up to two decimals");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        plans.changeAmount(plan.id(), clock.today(sheet), value);
        Person person = people.find(sheet, plan.person());
        audit.record("PLAN_CHANGED sheet=" + sheet + " by=" + user + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start() + " amount=" + value.toPlainString());
        // Its claims are on the sheet it generates them on (a descendant's, for the person of the same name there).
        String on = plan.sheet() == null ? sheet : plan.sheet();
        for (long id : plans.claimsMade(plan.id())) {
            Claim claim = claims.find(on, id);
            if (claim == null || claim.status() != ClaimStatus.DRAFT) {
                continue;
            }
            if (periods.closedOn(on, LocalDate.parse(claim.dueBy()))
                    || (!claim.lines().isEmpty() && new BigDecimal(claim.amount()).compareTo(value) != 0)) {
                continue;
            }
            Person owner = people.find(on, claim.person());
            LocalDate date = LocalDate.parse(claim.date());
            if (owner == null || owner.retiredBefore(date)) {
                continue;
            }
            ClaimInput input = new ClaimInput(value, date, claim.note(), claim.corrects(), claim.ref(), claim.tags());
            Rollup.Snapshot before = rollup.take(on);
            if (!claims.update(on, id, input, claim.tagsEver(), user)) {
                continue;
            }
            audit.record("CLAIM_EDITED sheet=" + on + " by=" + user + " " + input.auditFields(owner.name()));
            rollup.record(on, user, before);
        }
    }
}

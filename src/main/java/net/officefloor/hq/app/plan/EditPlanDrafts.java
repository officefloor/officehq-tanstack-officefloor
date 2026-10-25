package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAudit;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Gives the plan's new amount to the claims it generated that are still DRAFT, each as the claim form would edit it
 *  (recording who last edited it and writing CLAIM_EDITED), in the order they were generated. A draft the claim form
 *  would refuse the amount for keeps its own: one locked by another user, one whose due-by is in a closed period, one
 *  whose lines total another amount, and one dated after its person's retirement date. Claims in any other status keep
 *  theirs. */
public class EditPlanDrafts {
    public void edit(@Val Plan plan, PlanClaimRepository planClaims, ClaimRepository claims, PeriodRepository periods,
            ClaimLineRepository lines, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String userHeader, Audit audit) {
        String user = AuditUser.decode(userHeader);
        BigDecimal amount = plan.getAmount();
        planClaims.findByPlanId(plan.getId()).stream().sorted(Comparator.comparing(PlanClaim::getClaimId))
                .forEach(made -> claims.findById(made.getClaimId()).ifPresent(claim -> {
                    if (claim.getStatus() != ClaimStatus.DRAFT || claim.isLockedFor(user) || periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
                        return;
                    }
                    BigDecimal lined = lines.totalOf(claim);
                    if (lined != null && amount.compareTo(lined) != 0) {
                        return;
                    }
                    Person person = people.findById(claim.getPersonId()).orElse(null);
                    if (person != null && person.retiredOn(claim.getDate())) {
                        return;
                    }
                    claim.setAmount(amount);
                    claim.setEditedBy(user);
                    claims.saveAndFlush(claim);
                    audit.record(ClaimAudit.line("CLAIM_EDITED", claim, person == null ? "" : person.getName(), user));
                }));
    }
}

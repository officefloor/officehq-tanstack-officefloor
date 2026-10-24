package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Opens the claim's editor for the user: locks the claim for everyone else (and releases any other claim the user
 *  held). Refuses a claim that cannot be edited — not a DRAFT, or dated in a closed period — which takes no lock, and
 *  a claim another user holds locked. */
public class ApplyLock {
    public void apply(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository, PeriodRepository periods,
            @RequestHeader(name = "X-User", required = false) String user)
            throws ClaimStatusException, ClaimLockedException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimStatusException("Only a draft claim can be edited; void it and add a new one.");
        }
        if (periods.isClosedOn(sheet.getId(), claim.getDate())) {
            throw new ClaimStatusException("No claim can be edited in a closed period.");
        }
        String by = AuditUser.decode(user);
        if (claim.isLockedFor(by)) {
            throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy() + ".");
        }
        for (Claim other : repository.findBySheetIdAndLockedByIsNotNull(sheet.getId())) {
            if (by.equals(other.getLockedBy()) && !other.getId().equals(claim.getId())) {
                other.setLockedBy(null);
                repository.save(other);
            }
        }
        claim.setLockedBy(by);
        repository.saveAndFlush(claim);
    }
}

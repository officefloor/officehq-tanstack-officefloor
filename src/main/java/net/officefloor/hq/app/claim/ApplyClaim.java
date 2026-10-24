package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Saves the claim's changes; refuses them unless the claim is a DRAFT (an open claim is voided and added anew
 * instead), and when its date, or the one it would move to, is in a closed period. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, ClaimRepository repository,
            PeriodRepository periods) throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a draft claim can be edited; void it and add a new one.");
        }
        LocalDate date = input.date() != null ? input.date() : sheet.getToday();
        if (periods.isClosedOn(sheet.getId(), claim.getDate()) || periods.isClosedOn(sheet.getId(), date)) {
            throw new InvalidClaimException("No claim can be edited in a closed period.");
        }
        claim.setAmount(input.amount());
        claim.setDate(date);
        claim.setNote(input.note());
        repository.saveAndFlush(claim);
    }
}

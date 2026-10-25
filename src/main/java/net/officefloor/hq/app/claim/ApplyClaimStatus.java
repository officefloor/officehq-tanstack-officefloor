package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to the asked status, refusing a change its current status does not allow,
 * and refusing to let the user who opened it settle it. Opening records who opened it. Settling
 * fixes the claim's due at its person's grade's rate in force on the claim's date; without a rate its due stays live.
 */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, @Val ChangedBy by, Out<ClaimNumber> audited) throws BadRequestException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(status)) {
            throw new BadRequestException("A " + from + " claim cannot become " + status);
        }
        if (status == ClaimStatus.SETTLED && by.name().equals(claim.getOpenedBy())) {
            throw new BadRequestException("The user who opened a claim cannot settle it");
        }
        Integer had = claim.getNumber();
        claim.setStatus(status);
        if (status == ClaimStatus.VOID) {
            claim.setNumber(null);
        }
        if (status == ClaimStatus.OPEN) {
            claim.setOpenedBy(by.name());
        }
        if (status == ClaimStatus.SETTLED) {
            claim.setSettledDue(dueNow(claim, people, rates));
        }
        if (!repository.updateStatus(claim, from)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (status == ClaimStatus.OPEN) {
            // Numbered only once the claim is surely moving, so a refused open uses up no number.
            claim.setNumber(repository.nextNumber(claim.getSheetId()));
            repository.updateStatus(claim, status);
        }
        audited.set(new ClaimNumber(status == ClaimStatus.VOID ? had : claim.getNumber()));
    }

    /** The claim's amount times its person's grade's rate on the claim's date, rounded half to even; null when there is no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal rate = person == null ? null : rates.find(claim.getSheetId(), person.getGrade(), claim.getDate());
        return rate == null ? null
                : claim.getAmount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}

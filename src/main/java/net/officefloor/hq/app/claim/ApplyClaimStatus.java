package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to the asked status, refusing a change its current status does not allow. Settling
 * fixes the claim's due at its person's grade's rate now; without a rate its due stays live.
 */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, Out<ClaimNumber> audited) throws BadRequestException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(status)) {
            throw new BadRequestException("A " + from + " claim cannot become " + status);
        }
        Integer had = claim.getNumber();
        claim.setStatus(status);
        if (status == ClaimStatus.VOID) {
            claim.setNumber(null);
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

    /** The claim's amount times its person's grade's rate, rounded half up; null when there is no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal rate = person == null ? null : rates.find(claim.getSheetId(), person.getGrade());
        return rate == null ? null
                : claim.getAmount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
}

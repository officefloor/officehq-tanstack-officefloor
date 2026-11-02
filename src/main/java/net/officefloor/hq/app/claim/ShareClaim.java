package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Shares a claim with another person on the sheet, who holds a share of its due (a percent with up to two decimals,
 * more than 0.00): CLAIM_SHARED is written naming the claim and ending with=<person> share=<share>. Refuses the
 * claim's own person, a person already holding a share of it, shares totalling more than 100.00, and a SETTLED, VOID
 * or DELETED claim.
 */
public class ShareClaim {
    public void share(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody ShareRequest request, @Val Claim claim, Claims claims, People people, Audit audit,
            Rollup rollup) throws InvalidClaimStatusChangeException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID
                || claim.status() == ClaimStatus.DELETED) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be shared");
        }
        Person with = request == null || request.person() == null ? null : people.find(sheet, request.person());
        if (with == null) {
            throw new InvalidClaimStatusChangeException("Choose a person on the sheet to share the claim with");
        }
        if (with.id() == claim.person()) {
            throw new InvalidClaimStatusChangeException("A claim cannot be shared with its own person");
        }
        if (claim.shares().stream().anyMatch((s) -> s.person() == with.id())) {
            throw new InvalidClaimStatusChangeException(with.name() + " already holds a share of the claim");
        }
        String typed = request.share() == null ? "" : request.share().trim();
        if (!typed.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            throw new InvalidClaimStatusChangeException("A share is a percent with up to two decimals");
        }
        BigDecimal share = new BigDecimal(typed).setScale(2);
        if (share.signum() <= 0) {
            throw new InvalidClaimStatusChangeException("A share must be more than 0.00");
        }
        BigDecimal total = claim.shares().stream().map((s) -> new BigDecimal(s.share())).reduce(share, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            throw new InvalidClaimStatusChangeException("The claim's shares would total more than 100.00");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the status and the shares read, so a concurrent change is refused.
        if (!claims.share(sheet, claim.id(), claim.status(), claim.shares().size(), with.id(), share)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("CLAIM_SHARED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " with=" + with.name() + " share=" + share.toPlainString());
        rollup.record(sheet, user, before);
    }
}

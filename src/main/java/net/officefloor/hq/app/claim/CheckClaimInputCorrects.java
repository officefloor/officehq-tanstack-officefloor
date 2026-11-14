package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses (400) a correction unless the number it corrects is a SETTLED claim of the same person, and unless it is no
 * larger than what remains of that claim: its amount less the corrections already made against it (VOID or DELETED
 * ones left out).
 */
public class CheckClaimInputCorrects {
    public void check(@Val ClaimInput input, @Val Person person, ClaimRepository claims) throws InvalidClaimException {
        check(input, person, claims, null);
    }

    /** The check, leaving out of the corrections already made the claim with {@code editedId} (the one being edited). */
    static void check(ClaimInput input, Person person, ClaimRepository claims, Long editedId) throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        List<Claim> all = claims.findForPerson(person.sheetId(), person.id());
        Claim target = all.stream()
                .filter(c -> input.corrects().equals(c.number()) && c.status() == ClaimStatus.SETTLED)
                .findFirst().orElse(null);
        if (target == null) {
            throw new InvalidClaimException(
                    "A correction must name a settled claim of " + person.name() + "; no settled claim " + input.corrects() + ".");
        }
        BigDecimal remaining = target.amount();
        for (Claim c : all) {
            if (input.corrects().equals(c.corrects()) && !Objects.equals(c.id(), editedId)
                    && c.status() != ClaimStatus.VOID && c.status() != ClaimStatus.DELETED) {
                remaining = remaining.add(c.amount());
            }
        }
        if (input.amount().negate().compareTo(remaining) > 0) {
            throw new InvalidClaimException("A correction of " + input.corrects() + " may not exceed what remains of it: "
                    + remaining.max(BigDecimal.ZERO).setScale(2) + ".");
        }
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyShare {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // A claim that is not SETTLED, VOID or DELETED is shared with another person on the sheet: not its own person, nor
    // one already holding a share, at a share more than 0.00 that keeps its shares at no more than 100.00 in all.
    public void apply(@RequestBody(required = false) ShareRequest body, @Val Claim claim, Claims claims,
            ClaimShares shares, People people, Out<ClaimResponse> responseOut, Out<Shared> sharedOut)
            throws ClaimShareRefusedException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID
                || claim.status() == ClaimStatus.DELETED) {
            throw new ClaimShareRefusedException("A " + claim.status() + " claim cannot be shared.");
        }
        Person with = body == null || body.personId() == null ? null : people.find(claim.sheetId(), body.personId());
        if (with == null) {
            throw new ClaimShareRefusedException("Choose a person on the sheet to share the claim with.");
        }
        if (with.id() == claim.personId()) {
            throw new ClaimShareRefusedException("A claim cannot be shared with its own person.");
        }
        List<ClaimShare> held = shares.of(claim);
        if (held.stream().anyMatch(s -> s.personId() == with.id())) {
            throw new ClaimShareRefusedException(with.name() + " already holds a share of the claim.");
        }
        String typed = body.share() == null ? "" : body.share().trim();
        if (!SHARE.matcher(typed).matches()) {
            throw new ClaimShareRefusedException("The share must be a percent with up to two decimals.");
        }
        BigDecimal share = new BigDecimal(typed).setScale(2);
        if (share.signum() <= 0) {
            throw new ClaimShareRefusedException("The share must be more than 0.00.");
        }
        if (ClaimShares.total(held).add(share).compareTo(HUNDRED) > 0) {
            throw new ClaimShareRefusedException("The claim's shares would add up to more than 100.00.");
        }
        ClaimShare added = new ClaimShare(with.id(), share);
        shares.add(claim, added);
        sharedOut.set(new Shared(with.name(), added));
        responseOut.set(ClaimResponse.from(claims.find(claim.sheetId(), claim.id())));
    }
}

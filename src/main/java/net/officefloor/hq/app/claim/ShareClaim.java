package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Shares a claim with another person on the sheet ({@code with}, their id) at a percent ({@code share}, two
 *  decimals, more than 0.00): they hold that part of its due. Refused for a SETTLED, VOID or DELETED claim, for
 *  the claim's own person, a person already holding a share of it or one not on the sheet, and for a share
 *  that is not a percent more than 0.00 with up to two decimals or that would take its shares above 100.00. */
public class ShareClaim {
    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{0,2})?|\\.\\d{1,2}");
    private static final BigDecimal WHOLE = new BigDecimal("100.00");

    public void share(@RequestParam(name = "with", required = false) Long with,
            @RequestParam(name = "share", required = false) String share, @Val Claim claim,
            PersonRepository people, Out<ClaimShare> shared) throws ClaimStatusException, InvalidClaimException {
        String status = claim.getStatus();
        if (ClaimStatus.SETTLED.name().equals(status) || ClaimStatus.VOID.name().equals(status)
                || ClaimStatus.DELETED.name().equals(status)) {
            throw new ClaimStatusException("A " + status + " claim cannot be shared");
        }
        Person holder = with == null ? null : people.find(claim.getSheetId(), with);
        if (holder == null || holder.getId() == claim.getPersonId()) {
            throw new InvalidClaimException("A claim can only be shared with another person on the sheet");
        }
        if (claim.getShares().stream().anyMatch((s) -> s.personId() == holder.getId())) {
            throw new InvalidClaimException(holder.getName() + " already holds a share of the claim");
        }
        String s = share == null ? "" : share.trim();
        if (!SHARE.matcher(s).matches() || new BigDecimal(s).signum() <= 0) {
            throw new InvalidClaimException("A share is a percent more than 0.00 with up to two decimals");
        }
        BigDecimal pct = new BigDecimal(s).setScale(2);
        BigDecimal total = claim.getShares().stream().map((h) -> new BigDecimal(h.share()))
                .reduce(pct, BigDecimal::add);
        if (total.compareTo(WHOLE) > 0) {
            throw new InvalidClaimException("A claim's shares cannot add up to more than 100.00");
        }
        shared.set(new ClaimShare(holder.getId(), holder.getName(), pct.toPlainString()));
    }
}

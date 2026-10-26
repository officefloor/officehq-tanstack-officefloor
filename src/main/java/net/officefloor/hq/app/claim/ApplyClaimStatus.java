package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyClaimStatus {
    // Stored only if the claim still has the status it was loaded with, so two users cannot both move it.
    public void apply(@PathVariable("status") String status, @Val Claim claim, Claims claims,
            People people, Rates rates, Settings settings, Sheets sheets, ServerHttpConnection connection,
            Out<ClaimResponse> responseOut) throws ClaimStatusRefusedException, ClaimOverLimitException {
        ClaimStatus next;
        try {
            next = ClaimStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClaimStatusRefusedException("Not a claim status: " + status);
        }
        String user = Audit.user(connection).trim();
        // Four eyes: the user who opened a claim cannot settle it.
        if (next == ClaimStatus.SETTLED && claim.openedBy() != null && claim.openedBy().equals(user)) {
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it.");
        }
        if (!claim.status().allows(next)) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        // Settling freezes the late charge on the sheet's today into the due.
        BigDecimal due = settings.withLate(claim, due(claim, people, rates, settings), sheets.today(claim.sheetId()));
        // A clerk settles only a claim whose due is at or below the clerk limit; a due of n/a is within
        // any limit, and a steward settles anything.
        if (next == ClaimStatus.SETTLED && due != null) {
            BigDecimal limit = settings.clerkLimit(claim.sheetId());
            if (limit != null && due.compareTo(limit) > 0
                    && !Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
                throw new ClaimOverLimitException(
                        "A clerk cannot settle a claim whose due " + due.toPlainString() + " is above the clerk limit "
                                + limit.toPlainString() + ".");
            }
        }
        Claim changed = claims.changeStatus(claim, next, due, user);
        if (changed == null) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        responseOut.set(ClaimResponse.from(changed));
    }

    /**
     * The claim's due now, before its late charge: the figure its person's last statement bound it to, otherwise its amount times its
     * person's grade's rate on the claim's date, half to even, raised to the floor; null when none applies.
     */
    private static BigDecimal due(Claim claim, People people, Rates rates, Settings settings) {
        if (claim.stated()) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person == null) {
            return null;
        }
        BigDecimal rate = rates.on(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null : settings.floored(claim.sheetId(), claim.amount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN));
    }
}

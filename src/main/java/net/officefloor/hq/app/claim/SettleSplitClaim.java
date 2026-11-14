package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Settles the claim just split off, as the settle button does (its checks were made on the original): its due is
 * fixed at its figure now, or, when the user's role may not settle that due (the clerk limit), it goes PENDING,
 * live and unfixed. Publishes the change for the settle's save, log and audit steps, taken one at a time (no batch).
 */
public class SettleSplitClaim {
    public void settle(@RequestParam("user") String user, @Val Claim claim, SheetRepository sheets,
            PersonRepository people, RateRepository rates, SettingRepository settings, ClaimRepository claims,
            Out<ClaimAction> applied, Out<ClaimNumber> number, Out<PreviousDue> previous,
            Out<ClaimActionRequest> request, Out<Claim> updated) {
        ClaimAction action = ClaimAction.SETTLE;
        BigDecimal due = ClaimDueNow.of(claim, sheets, people, rates, settings, claims);
        if (!settings.find(claim.sheetId()).maySettle(user, due)) {
            action = ClaimAction.PEND;
            due = null;
        }
        applied.set(action);
        previous.set(new PreviousDue(claim.due(), false));
        number.set(new ClaimNumber(claim.number()));
        request.set(new ClaimActionRequest("", ""));
        updated.set(claim.withStatus(action.to(), claim.number(), due, claim.openedBy()));
    }
}

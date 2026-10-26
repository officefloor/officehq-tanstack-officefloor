package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyResettle {
    // Only a steward re-settles, and only a SETTLED claim. The new figure is the one the claim would have today as an
    // open claim bound by no statement: its amount times its person's grade's rate on its date (as they stand now),
    // rounded by the policy date, raised to the floor, with the late charge on the sheet's today added on top.
    public void apply(@Val Claim claim, Claims claims, People people, Rates rates, Settings settings, Sheets sheets,
            ServerHttpConnection connection, Out<ClaimResponse> responseOut, Out<Resettled> resettledOut)
            throws ClaimResettleRefusedException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
            throw new ClaimResettleRefusedException("Only a steward can re-settle a claim.");
        }
        if (claim.status() != ClaimStatus.SETTLED) {
            throw new ClaimResettleRefusedException("Only a SETTLED claim can be re-settled.");
        }
        BigDecimal due = null;
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person != null) {
            BigDecimal rate = rates.on(claim.sheetId(), person.gradeOn(claim.date()), claim.date());
            Claim open = claim.withStatus(ClaimStatus.OPEN, claim.number(), null, claim.openedBy());
            due = settings.withLate(open, settings.live(claim, rate), sheets.today(claim.sheetId()));
        }
        Claim changed = claims.resettle(claim, due);
        if (changed == null) {
            throw new ClaimResettleRefusedException("Only a SETTLED claim can be re-settled.");
        }
        claims.log(changed, user, sheets.today(claim.sheetId()), due);
        responseOut.set(ClaimResponse.from(changed));
        resettledOut.set(new Resettled(claim.settledDue(), due));
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyWaive {
    // Only a steward waives, and only an OPEN or PENDING claim not already waived. Stored only if the claim is still
    // OPEN or PENDING and not waived, so a claim settled or waived meanwhile by another user is refused.
    public void apply(@Val Claim claim, Claims claims, Settings settings, ServerHttpConnection connection,
            Out<ClaimResponse> responseOut) throws ClaimWaiveRefusedException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
            throw new ClaimWaiveRefusedException("Only a steward can waive a claim's late charge.");
        }
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new ClaimWaiveRefusedException("Only an OPEN or PENDING claim's late charge can be waived.");
        }
        if (claim.waived()) {
            throw new ClaimWaiveRefusedException("The claim's late charge is already waived.");
        }
        Claim waived = claims.waive(claim);
        if (waived == null) {
            throw new ClaimWaiveRefusedException("The claim changed meanwhile: try again.");
        }
        responseOut.set(ClaimResponse.from(waived));
    }
}

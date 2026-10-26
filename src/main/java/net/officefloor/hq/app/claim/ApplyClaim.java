package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyClaim {
    // Claim is a record, so the edited claim is published in place of the loaded one.
    public void apply(@Val ClaimInput input, @Val Claim claim, Claims claims, Sheets sheets,
            ServerHttpConnection connection, Out<ClaimResponse> responseOut) {
        Claim edited = claim.with(input, input.date() != null ? input.date() : sheets.today(claim.sheetId()));
        claims.save(edited, Audit.user(connection).trim());
        responseOut.set(ClaimResponse.from(edited));
    }
}

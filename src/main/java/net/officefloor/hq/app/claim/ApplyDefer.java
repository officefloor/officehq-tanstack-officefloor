package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyDefer {
    // Only an OPEN claim is deferred, and only to a date after the sheet's today. Stored only if the claim is still
    // OPEN, so a claim settled or voided meanwhile by another user is refused.
    public void apply(@RequestBody(required = false) DeferRequest body, @Val Claim claim, Claims claims, Sheets sheets,
            Out<ClaimResponse> responseOut) throws ClaimDeferRefusedException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new ClaimDeferRefusedException("Only an OPEN claim can be deferred.");
        }
        LocalDate until;
        try {
            until = LocalDate.parse(body == null || body.until() == null ? "" : body.until().trim());
        } catch (DateTimeParseException e) {
            throw new ClaimDeferRefusedException("Defer to a date (yyyy-mm-dd).");
        }
        LocalDate today = sheets.today(claim.sheetId());
        if (!until.isAfter(today)) {
            throw new ClaimDeferRefusedException("A claim is deferred only to a date after today (" + today + ").");
        }
        Claim deferred = claims.defer(claim, until);
        if (deferred == null) {
            throw new ClaimDeferRefusedException("Only an OPEN claim can be deferred.");
        }
        responseOut.set(ClaimResponse.from(deferred));
    }
}

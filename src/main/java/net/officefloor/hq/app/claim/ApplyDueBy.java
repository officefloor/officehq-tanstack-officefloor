package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyDueBy {
    // Only a steward sets a due-by, and only on a DRAFT, OPEN or PENDING claim, to a real date not inside a closed period
    // (it may be before the claim's date). It is kept as set: a holiday added later does not work it out again.
    public void apply(@RequestBody(required = false) DueByRequest body, @Val Claim claim, Claims claims, Periods periods,
            Settings settings, ServerHttpConnection connection, Out<ClaimResponse> responseOut)
            throws ClaimDueBySetRefusedException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
            throw new ClaimDueBySetRefusedException("Only a steward can set a claim's due-by.");
        }
        if (claim.status() != ClaimStatus.DRAFT && !claim.status().isOpen()) {
            throw new ClaimDueBySetRefusedException("Only a DRAFT, OPEN or PENDING claim can have its due-by set.");
        }
        LocalDate dueBy;
        try {
            dueBy = LocalDate.parse(body == null || body.dueBy() == null ? "" : body.dueBy().trim());
        } catch (DateTimeParseException e) {
            throw new ClaimDueBySetRefusedException("Set the due-by to a date (yyyy-mm-dd).");
        }
        Period closed = periods.closedOn(claim.sheetId(), dueBy);
        if (closed != null) {
            throw new ClaimDueBySetRefusedException("The period " + closed.name() + " is closed: no claim can be moved into it.");
        }
        Claim set = claims.setDueBy(claim, dueBy);
        if (set == null) {
            throw new ClaimDueBySetRefusedException("The claim has changed since it was loaded.");
        }
        responseOut.set(ClaimResponse.from(set));
    }
}

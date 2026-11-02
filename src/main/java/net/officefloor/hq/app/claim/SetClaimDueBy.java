package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward sets one claim's due-by by the rules of {@link ClaimDueBySetter}. */
public class SetClaimDueBy {
    public void set(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate dueBy,
            @Val Claim claim, ClaimDueBySetter setter) throws InvalidClaimStatusChangeException {
        setter.set(sheet, user, claim, dueBy);
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward re-dates one claim by the rules of {@link ClaimRedater}. */
public class RedateClaim {
    public void redate(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate date,
            @Val Claim claim, ClaimRedater redater) throws InvalidClaimStatusChangeException {
        redater.redate(sheet, user, claim, date);
    }
}

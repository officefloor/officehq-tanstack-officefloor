package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Splits an amount off an OPEN claim into a new OPEN claim, as {@link ClaimSplitter} does. */
public class SplitClaim {
    public void split(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val BigDecimal amount,
            @Val Claim claim, ClaimSplitter splitter) throws InvalidClaimStatusChangeException {
        splitter.split(sheet, user, claim, amount);
    }
}

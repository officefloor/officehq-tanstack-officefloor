package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Saves both claims of a split, and logs the new claim's status. */
public class SaveSplit {
    public void save(@RequestParam(name = "by", required = false) String by, @Val Split split,
            ClaimRepository claims, ClaimLogRepository log) {
        claims.update(split.original());
        claims.updateBinding(split.original());
        claims.update(split.created());
        claims.updateSplitFrom(split.created());
        log.record(split.created(), by);
    }
}

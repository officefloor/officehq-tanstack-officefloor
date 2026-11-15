package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** After the claim's own line, before the rollup lines: a claim now SETTLED notifies its person's watchers. */
public class RecordClaimNotify {
    public void record(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, WatchRepository watches, Audit audit) {
        if (ClaimStatus.SETTLED.name().equals(claim.getStatus())) {
            Notify.watchers(by, claim, people, watches, audit);
        }
    }
}

package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumber;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Follows a claim's own record, when the claim was just settled, approved or re-settled (it ends SETTLED), with one
 * NOTIFY line per user watching its person, in the order they started watching. A clerk's settle that went PENDING
 * notifies no one.
 */
public class NotifyWatchers {
    public void audit(@Val Claim claim, @Val ClaimNumber number, PersonRepository people, WatchRepository watches,
            @RequestParam("user") String user, Audit audit) {
        if (claim.status() != ClaimStatus.SETTLED) {
            return;
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        String name = person == null ? "" : person.name();
        for (String watcher : watches.watchers(claim.sheetId(), claim.personId())) {
            audit.record("NOTIFY sheet=" + claim.sheetId() + " by=" + user + " user=" + watcher + " person=" + name
                    + Claim.auditNumber(number.value()));
        }
    }
}

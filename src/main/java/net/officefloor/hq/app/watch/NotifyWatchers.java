package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** After the claim's own line: when the claim has just been settled (settled, approved or re-settled), one line
 *  for each user watching its person, in the order they started watching. */
public class NotifyWatchers {
    public void send(@Val Claim claim, PersonRepository people, WatchRepository watches,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            return;
        }
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        for (Watch watch : watches.findByPersonIdOrderByIdAsc(claim.getPersonId())) {
            audit.record("NOTIFY sheet=" + claim.getSheetId() + " by=" + AuditUser.decode(user) + " user="
                    + watch.getUserName() + " person=" + person + " number=" + claim.getNumber());
        }
    }
}

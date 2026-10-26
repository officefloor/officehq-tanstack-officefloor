package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class NotifyWatchers {
    // After the claim's own line: when the claim is now SETTLED (settled, approved or re-settled), one NOTIFY line per
    // user watching its person, in the order they started watching. A claim gone PENDING or anywhere else notifies no one.
    public void notifyWatchers(@Val Claim loaded, @Val ClaimResponse changed, People people, Watches watches,
            ServerHttpConnection connection, Audit audit) {
        if (!ClaimStatus.SETTLED.name().equals(changed.status())) {
            return;
        }
        Person person = people.find(loaded.sheetId(), loaded.personId());
        if (person == null) {
            return;
        }
        String number = changed.number() != null ? changed.number() : loaded.number();
        for (String watcher : watches.watchers(person.id())) {
            audit.record(Audit.line("NOTIFY", loaded.sheetId(), Audit.user(connection), "user=" + watcher,
                    "person=" + person.name(), "number=" + (number == null ? "" : number)));
        }
    }
}

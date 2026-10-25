package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.session.ChangedBy;

/** One NOTIFY line per user watching the claim's person, in the order they started watching. */
final class ClaimNotify {

    private ClaimNotify() {
    }

    static void watchers(Claim claim, Person person, String number, ChangedBy by, WatchRepository watches, Audit audit) {
        if (person == null) {
            return;
        }
        for (String watcher : watches.watchers(person.getId())) {
            audit.record("NOTIFY sheet=" + claim.getSheetId() + " by=" + by.name() + " user=" + watcher + " person="
                    + person.getName() + " number=" + number);
        }
    }
}

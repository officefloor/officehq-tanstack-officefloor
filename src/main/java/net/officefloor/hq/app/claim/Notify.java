package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;

/** A claim just settled, approved or re-settled: one NOTIFY line per user watching its person, in the
 *  order they started watching. */
final class Notify {
    private Notify() {
    }

    static void watchers(String by, Claim claim, PersonRepository people, WatchRepository watches, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person == null) {
            return;
        }
        for (String watcher : watches.watchers(claim.getSheetId(), person.getId())) {
            audit.record("NOTIFY sheet=" + claim.getSheetId() + " by=" + (by == null ? "" : by)
                    + " user=" + watcher + " person=" + person.getName() + claim.getAuditNumber());
        }
    }
}

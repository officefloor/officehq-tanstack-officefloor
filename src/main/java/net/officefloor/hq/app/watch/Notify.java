package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;

/** Tells the person's watchers of a claim of theirs settled, approved or re-settled: one line
 *  NOTIFY sheet=<id> by=<user> user=<watcher> person=<name> number=<n> per watcher, in the order they
 *  started watching. */
public final class Notify {
    private Notify() {
    }

    public static void watchers(WatchStore watches, Audit audit, Person person, String by, Object number) {
        for (String watcher : watches.watchers(person.getSheetId(), person.getId())) {
            audit.record("NOTIFY sheet=" + person.getSheetId() + " by=" + by + " user=" + watcher + " person="
                    + person.getName() + " number=" + number);
        }
    }
}

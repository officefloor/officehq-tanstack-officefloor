package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Starts or stops the signed-in user watching the person; asking for the state already held changes nothing. */
public class WatchPerson {
    public void watch(@Val WatchRequest request, @Val Person person, Watches watches, Audit audit) {
        String sheetId = person.getSheetId();
        boolean watching = watches.isWatching(sheetId, person.getId(), request.user());
        if (watching == request.on()) {
            return;
        }
        if (request.on()) {
            watches.watch(sheetId, person.getId(), request.user());
        } else {
            watches.unwatch(sheetId, person.getId(), request.user());
        }
        audit.recordOnCommit((request.on() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + sheetId
                + " by=" + request.user() + " name=" + person.getName());
    }
}

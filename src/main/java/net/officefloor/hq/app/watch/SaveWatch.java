package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Start or stop the user watching the loaded person; asking for the state already held changes nothing. */
public class SaveWatch {
    public void save(@Val WatchInput input, @Val Person loaded, WatchRepository watches, Out<WatchResponse> saved) {
        boolean now = watches.watching(loaded.sheetId(), loaded.id(), input.user());
        if (now == input.watched()) {
            saved.set(new WatchResponse(loaded.id(), now, false));
            return;
        }
        if (input.watched()) {
            watches.watch(loaded.sheetId(), loaded.id(), input.user());
        } else {
            watches.unwatch(loaded.sheetId(), loaded.id(), input.user());
        }
        saved.set(new WatchResponse(loaded.id(), input.watched(), true));
    }
}

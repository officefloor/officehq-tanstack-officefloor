package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Starts the user watching the person, or stops them when they already were. */
public class ToggleWatch {
    public void toggle(@Val WatchRequest request, @Val Person person, Watches watches, Out<WatchChange> change) {
        boolean was = watches.watches(person.getSheetId(), person.getId(), request.user());
        if (was) watches.unwatch(person.getSheetId(), person.getId(), request.user());
        else watches.watch(person.getSheetId(), person.getId(), request.user());
        change.set(new WatchChange(request.user(), !was));
    }
}

package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Starts the user watching the person, or stops them when they already do. */
public class ToggleWatch {
    public void toggle(@Val WatchRequest request, @Val Person person, WatchStore watches, Out<Watching> toggled) {
        boolean watched = !watches.watches(person.getSheetId(), person.getId(), request.user());
        if (watched) {
            watches.watch(person.getSheetId(), person.getId(), request.user());
        } else {
            watches.unwatch(person.getSheetId(), person.getId(), request.user());
        }
        toggled.set(new Watching(request.user(), watched));
    }
}

package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Starts the signed-in user watching the person, or stops them when they already do. */
public class ToggleWatch {
    public void toggle(@Val Person person, @RequestParam("user") String user, WatchRepository watches,
            Out<Watching> watching) {
        if (watches.exists(person.sheetId(), person.id(), user)) {
            watches.unwatch(person.sheetId(), person.id(), user);
            watching.set(new Watching(false));
        } else {
            watches.watch(person.sheetId(), person.id(), user);
            watching.set(new Watching(true));
        }
    }
}

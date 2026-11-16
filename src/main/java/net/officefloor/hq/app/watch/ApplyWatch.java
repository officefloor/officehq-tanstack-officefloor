package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Starts or stops the user watching the person; starting to watch one already watched, or stopping watching one not
 *  watched, is refused. */
public class ApplyWatch {
    public void apply(@Val Person person, @Val WatchChange change, WatchRepository watches) throws InvalidPersonException {
        boolean done = change.on() ? watches.add(person.sheetId(), person.id(), change.by())
                : watches.remove(person.sheetId(), person.id(), change.by());
        if (!done) {
            throw new InvalidPersonException(change.by() + (change.on() ? " already watches " : " does not watch ")
                    + person.name());
        }
    }
}

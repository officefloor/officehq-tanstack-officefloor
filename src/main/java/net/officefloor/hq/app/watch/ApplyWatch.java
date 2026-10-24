package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Starts or stops the signed-in user watching the person; says whether that changed anything (watching one
 *  already watched, or stopping one not watched, changes nothing). */
public class ApplyWatch {
    public void apply(@Val WatchInput input, @Val Person person, WatchRepository watches, Out<WatchChange> changed) {
        var existing = watches.findByPersonIdAndUserName(person.getId(), input.user());
        if (input.on() && existing.isEmpty()) {
            watches.saveAndFlush(new Watch(person.getSheetId(), person.getId(), input.user()));
            changed.set(new WatchChange(true));
        } else if (!input.on() && existing.isPresent()) {
            watches.delete(existing.get());
            watches.flush();
            changed.set(new WatchChange(true));
        } else {
            changed.set(new WatchChange(false));
        }
    }
}

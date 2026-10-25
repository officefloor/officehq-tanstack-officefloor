package net.officefloor.hq.app.person;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Starts or stops the signed-in user watching the person. */
public class ApplyWatch {
    public void apply(@Val WatchInput watch, @Val Person person, @Val ChangedBy by, WatchRepository watches,
            Out<WatchChanged> changed) {
        boolean done = watch.on() ? watches.watch(person.getId(), by.name()) : watches.unwatch(person.getId(), by.name());
        changed.set(new WatchChanged(done));
    }
}

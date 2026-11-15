package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Starts or stops the signed-in user watching the person; nothing changes when that is already so. */
public class WatchPerson {
    public void watch(@RequestParam(name = "by", required = false) String by, @Val WatchRequest request,
            @Val Person person, WatchRepository watches, Out<Watch> watch) {
        boolean on = request.on();
        boolean changed = watches.set(person, by == null ? "" : by, on);
        watch.set(new Watch(person.getId(), on, changed));
    }
}

package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Starts or stops the signed-in user watching the person; asking for the state already held writes nothing. */
public class WatchPerson {
    public void watch(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @RequestBody WatchRequest request, Watches watches, Audit audit) {
        boolean on = request == null || request.on() == null || request.on();
        if (on) {
            if (watches.watch(sheet, person.id(), user)) {
                audit.record("PERSON_WATCHED sheet=" + sheet + " by=" + user + " name=" + person.name());
            }
        } else if (watches.unwatch(sheet, person.id(), user)) {
            audit.record("PERSON_UNWATCHED sheet=" + sheet + " by=" + user + " name=" + person.name());
        }
    }
}

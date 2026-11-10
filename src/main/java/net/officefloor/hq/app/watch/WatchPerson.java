package net.officefloor.hq.app.watch;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The signed-in user starts or stops watching the person. Already so: nothing changes. */
public class WatchPerson {
    public void watch(@Val WatchInput input, @Val Sheet sheet, @Val Person person, WatchRepository watches,
            HttpServletRequest request, Out<WatchChange> change) {
        String user = Audit.user(request);
        boolean was = watches.watches(sheet.id(), person.getId(), user);
        if (was == input.on()) {
            change.set(new WatchChange(was, false));
            return;
        }
        if (input.on()) watches.watch(sheet.id(), person.getId(), user);
        else watches.unwatch(sheet.id(), person.getId(), user);
        change.set(new WatchChange(input.on(), true));
    }
}

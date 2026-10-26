package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyWatch {
    // Only a change is stored and recorded: watching a person already watched (or unwatching one not watched) does nothing.
    public void apply(@Val WatchRequest request, @Val Person person, Watches watches, ServerHttpConnection connection,
            Audit audit) throws InvalidPersonException {
        String user = Audit.user(connection).trim();
        if (user.isEmpty()) {
            throw new InvalidPersonException("Sign in to watch.");
        }
        boolean watched = watches.has(person.id(), user);
        if (request.watched() == watched) {
            return;
        }
        if (request.watched()) {
            watches.add(person.id(), user);
        } else {
            watches.remove(person.id(), user);
        }
        audit.record(Audit.line(request.watched() ? "PERSON_WATCHED" : "PERSON_UNWATCHED", person.sheetId(), user,
                "name=" + person.name()));
    }
}

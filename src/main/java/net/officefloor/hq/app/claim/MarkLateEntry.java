package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Marks the claim just added a late entry when its period is soft-closed (see {@link LateEntries}). */
public class MarkLateEntry {
    public void mark(@Val Claim claim, @Val Person person, @RequestParam("user") String user, LateEntries lateEntries) {
        lateEntries.mark(claim, person, user);
    }
}

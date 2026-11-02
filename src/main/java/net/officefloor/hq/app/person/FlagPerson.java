package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Flags the person, replacing any flag they had: until its date only a steward may open a claim of theirs. */
public class FlagPerson {
    public void flag(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val Flag flag, People people, Audit audit) {
        people.flag(person.id(), flag.reason(), flag.until());
        audit.record("PERSON_FLAGGED sheet=" + sheet + " by=" + user + " name=" + person.name() + " reason="
                + flag.reason() + " until=" + flag.until());
    }
}

package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Swaps the person with the one above or below them in the list; the top cannot move up nor the bottom down. */
public class MovePerson {
    public void move(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val Boolean up, People people, Audit audit) throws InvalidPersonException {
        List<Person> everyone = people.list(sheet);
        int at = -1;
        for (int i = 0; i < everyone.size(); i++) {
            if (everyone.get(i).id() == person.id()) {
                at = i;
            }
        }
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= everyone.size()) {
            throw new InvalidPersonException(person.name() + " is already at the " + (up ? "top" : "bottom"));
        }
        people.swapPositions(sheet, person.id(), everyone.get(to).id());
        audit.record("PERSON_MOVED sheet=" + sheet + " by=" + user + " name=" + person.name() + " position=" + (to + 1));
    }
}

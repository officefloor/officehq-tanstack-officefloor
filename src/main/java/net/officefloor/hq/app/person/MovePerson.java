package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with the one above or below them in the sheet's list; the first cannot move up,
 *  nor the last down. */
public class MovePerson {
    public void move(@Val MoveRequest request, @Val Person person, PersonRepository people,
            Out<PersonMoved> moved) throws InvalidPersonException {
        List<Person> listed = people.list(person.getSheetId());
        int at = -1;
        for (int i = 0; i < listed.size(); i++) {
            if (listed.get(i).getId() == person.getId()) at = i;
        }
        int to = "up".equals(request.direction()) ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= listed.size()) {
            throw new InvalidPersonException("The person cannot move " + request.direction());
        }
        people.swap(person.getSheetId(), person.getId(), listed.get(to).getId());
        moved.set(new PersonMoved(to + 1));
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Swaps the person with the one above (up) or below (down) them in the list; the top person cannot move up nor the
 * bottom one down (400). Publishes their new position, counting from 1.
 */
public class MovePerson {
    public void move(@Val Person person, @RequestParam("direction") String direction, PersonRepository people,
            Out<PersonMoved> moved) throws InvalidPersonException {
        boolean up = "up".equals(direction);
        if (!up && !"down".equals(direction)) {
            throw new InvalidPersonException("A person moves up or down.");
        }
        int position = people.move(person, up);
        if (position == 0) {
            throw new InvalidPersonException(up ? "The top person cannot move up." : "The bottom person cannot move down.");
        }
        moved.set(new PersonMoved(person, position));
    }
}

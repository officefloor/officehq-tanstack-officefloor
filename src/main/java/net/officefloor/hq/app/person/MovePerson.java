package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Swaps the person with the one above or below them in the list. The top row cannot move up nor
 * the bottom row down: refused, writing nothing.
 */
public class MovePerson {
    public void move(@Val MoveInput input, @Val Person person, PersonRepository repository, Out<Moved> moved)
            throws InvalidPersonException {
        Integer position = repository.move(person, input.up());
        if (position == null) {
            throw new InvalidPersonException(input.up() ? "The top row cannot move up" : "The bottom row cannot move down");
        }
        moved.set(new Moved(position));
    }
}

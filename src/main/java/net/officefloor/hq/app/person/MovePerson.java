package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with the one above or below them in the list; refuses moving the top up or the bottom down. */
public class MovePerson {
    public void move(@Val MoveInput input, @Val Person person, PersonRepository repository, Out<PersonMoved> moved)
            throws BadRequestException {
        Integer position = repository.move(person, input.up());
        if (position == null) {
            throw new BadRequestException(input.up() ? "That person is at the top of the list already"
                    : "That person is at the bottom of the list already");
        }
        moved.set(new PersonMoved(position));
    }
}

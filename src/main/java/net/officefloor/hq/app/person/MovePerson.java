package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Swaps the person with the one above or below them in the list; the top one cannot move up nor the bottom one down. */
public class MovePerson {
    public void move(@Val Person person, @Val PersonMove move, PersonRepository people) throws InvalidPersonException {
        Integer position = people.move(person.sheetId(), person.id(), move.up());
        if (position == null) {
            throw new InvalidPersonException(person.name() + " is already at the " + (move.up() ? "top" : "bottom"));
        }
        move.moved(position);
    }
}

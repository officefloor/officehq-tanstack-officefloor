package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with the one above or below them in the list, answering where it leaves them
 *  (counting from 1). Refused: the top row moving up, or the bottom row down. */
public class MovePerson {
    public Integer move(@Val Move move, @Val Person person, People people) throws InvalidSheetRequestException {
        if (!people.move(person, move.up()))
            throw new InvalidSheetRequestException(person.getName() + " is already " + (move.up() ? "first" : "last"));
        return people.positionOf(person);
    }
}

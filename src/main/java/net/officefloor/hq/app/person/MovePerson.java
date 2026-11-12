package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with their neighbour above or below; refused when they are already at that end. */
public class MovePerson {
    public void move(@Val MoveInput input, @Val Person person, PersonStore store, Out<Moved> moved)
            throws InvalidSheetException {
        int position = store.move(person, input.up());
        if (position == 0) {
            throw new InvalidSheetException(input.up() ? "The first person cannot move up" : "The last person cannot move down");
        }
        moved.set(new Moved(position));
    }
}

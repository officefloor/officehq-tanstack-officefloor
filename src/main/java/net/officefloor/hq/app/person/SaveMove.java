package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Swap the loaded person with the one above or below them; refused (nothing written) when they are already at
 *  the top (up) or the bottom (down) of the list. */
public class SaveMove {
    public void save(@Val MoveInput input, @Val Person loaded, PersonRepository repository, Out<MoveResult> moved)
            throws InvalidPersonException {
        int position = repository.move(loaded, input.up());
        if (position == 0) {
            throw new InvalidPersonException(loaded.name() + " is already at the "
                    + (input.up() ? "top" : "bottom") + " of the list.");
        }
        moved.set(new MoveResult(position));
    }
}

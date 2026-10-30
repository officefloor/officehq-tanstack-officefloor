package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Swaps the person with their neighbour above or below in the list; the top person cannot move up nor the
 *  bottom person down. */
public class MovePerson {
    public void move(@Val MoveRequest request, @Val Person person, People people, Audit audit) throws InvalidPersonException {
        int position = people.move(person, request.up());
        if (position == 0) {
            throw new InvalidPersonException(request.up() ? "The person is already at the top of the list"
                    : "The person is already at the bottom of the list");
        }
        audit.recordOnCommit("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " position=" + position);
    }
}

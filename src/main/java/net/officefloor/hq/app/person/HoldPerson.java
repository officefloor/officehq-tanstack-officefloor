package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Puts the person on hold or takes them off it; refuses when they are in that state already. */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Person person, PersonRepository repository)
            throws BadRequestException {
        if (person.isHeld() == input.held() || !repository.hold(person.getId(), input.held())) {
            throw new BadRequestException(input.held() ? "That person is already on hold; reload the sheet"
                    : "That person is not on hold; reload the sheet");
        }
        person.setHeld(input.held());
    }
}

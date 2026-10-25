package net.officefloor.hq.app.person;

import net.officefloor.hq.app.ConflictException;
import net.officefloor.plugin.variable.Val;

/** Saves the edit, refusing it (nothing written) when the person was saved by someone else since
 *  the form loaded it. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository repository)
            throws ConflictException {
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
        person.setName(request.name());
        person.setGrade(request.grade());
        if (!repository.update(person)) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
    }
}

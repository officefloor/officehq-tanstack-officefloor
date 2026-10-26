package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Merges the person into another on the sheet: only a steward may, never into the person themselves, and neither of
 * the two may be retired. See {@link PersonRepository#merge} for what moves and what is dropped.
 */
public class MergePerson {
    public void merge(@Val MergeInput input, @Val Person person, @Val ChangedBy by, PersonRepository repository,
            RoleRepository roles, Out<PersonMerged> merged) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(person.getSheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may merge people");
        }
        Person into = repository.find(person.getSheetId(), input.intoId());
        if (into == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (into.getId() == person.getId()) {
            throw new BadRequestException("A person cannot be merged into themselves");
        }
        if (person.isRetired() || into.isRetired()) {
            throw new BadRequestException("A retired person cannot be merged");
        }
        if (!repository.merge(person, into)) {
            throw new BadRequestException("Someone else has changed these people; reload the sheet");
        }
        merged.set(new PersonMerged(person.getName(), into.getName()));
    }
}

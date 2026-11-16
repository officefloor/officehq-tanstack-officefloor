package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Checks a merge: only a steward merges, and only into another person on the sheet; a merge into the same person, or
 *  with either of them retired (whatever the date), is refused. */
public class CheckMerge {
    public void check(@Val Person person, @Val MergeRequest request, PersonRepository people, RoleRepository roles,
            Out<PersonMerge> merge) throws InvalidPersonException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(person.sheetId(), request.by()))) {
            throw new InvalidPersonException("Only a steward merges people");
        }
        Person into = people.find(person.sheetId(), request.intoId());
        if (into == null || into.id() == person.id()) {
            throw new InvalidPersonException("A person merges into another person on the sheet");
        }
        if (person.retired() || into.retired()) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        merge.set(new PersonMerge(person, into, request.by()));
    }
}

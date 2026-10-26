package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person to the pool with their share; a person already in the pool is refused. */
public class AddPoolMember {
    public void add(@Val SheetResponse sheet, @Val PoolMemberInput input, PoolRepository pools, PersonRepository people,
            Out<PoolMemberAdded> added) throws BadRequestException {
        Long poolId = pools.findId(sheet.id(), input.pool());
        if (poolId == null) {
            throw new BadRequestException("No such pool on this sheet");
        }
        Person person = people.find(sheet.id(), input.personId());
        if (person == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (!pools.addMember(sheet.id(), poolId, person.getId(), input.share())) {
            throw new BadRequestException(person.getName() + " is already in this pool");
        }
        added.set(new PoolMemberAdded(sheet.id(), input.pool(), person.getName(), input.share()));
    }
}

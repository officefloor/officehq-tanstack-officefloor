package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A pool or person not on the sheet, or a person already in the pool, is refused. */
public class SavePoolMember {
    public void save(@Val PoolMemberInput input, @Val Sheet sheet, PoolRepository pools, PersonRepository people,
            Out<PoolMember> memberOut) throws InvalidPoolException {
        Pool pool = pools.find(sheet.id(), input.pool());
        if (pool == null) throw new InvalidPoolException("No pool " + input.pool());
        Person person = people.find(sheet.id(), input.personId());
        if (person == null) throw new InvalidPoolException("No such person");
        if (pools.isMember(pool, person.getId())) {
            throw new InvalidPoolException(person.getName() + " is already in " + pool.name());
        }
        pools.addMember(pool, person.getId(), input.share());
        memberOut.set(new PoolMember(sheet.id(), pool.name(), person.getId(), person.getName(), input.share(),
                person.isRetired()));
    }
}

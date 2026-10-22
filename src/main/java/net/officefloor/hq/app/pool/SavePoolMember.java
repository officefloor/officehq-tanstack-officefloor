package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the member, refusing an unknown pool or person, or a person already in the pool. */
public class SavePoolMember {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("poolId") String poolId,
            @Val PoolMemberInput input, PeriodRepository periods, PoolRepository repository, PersonRepository people,
            Out<PoolMember> member) throws InvalidPoolException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        Pool pool;
        try {
            pool = repository.find(id, Long.parseLong(poolId.trim()));
        } catch (NumberFormatException e) {
            pool = null;
        }
        if (pool == null) {
            throw new InvalidPoolException("No pool " + poolId + " on this sheet.");
        }
        Person person = people.find(id, input.personId());
        if (person == null) {
            throw new InvalidPoolException("No person " + input.personId() + " on this sheet.");
        }
        if (repository.isMember(pool, person.id())) {
            throw new InvalidPoolException(person.name() + " is already in " + pool.name() + ".");
        }
        member.set(repository.addMember(pool, person, input));
    }
}

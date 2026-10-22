package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the member, refusing an unknown pool or person, or a person already in the pool. A member pool is refused
 *  when it is already a member of a pool, when it is the pool itself or contains it through any chain, or when the
 *  chain of pools would be more than three deep. */
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
        if (input.memberPoolId() != null) {
            member.set(repository.addMember(pool, memberPool(id, pool, input.memberPoolId(), repository), input));
            return;
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

    private static final int MAX_DEPTH = 3;

    private static Pool memberPool(String sheetId, Pool pool, long memberPoolId, PoolRepository repository)
            throws InvalidPoolException {
        Pool child = repository.find(sheetId, memberPoolId);
        if (child == null) {
            throw new InvalidPoolException("No pool " + memberPoolId + " on this sheet.");
        }
        if (child.id() == pool.id()) {
            throw new InvalidPoolException("A pool cannot be a member of itself.");
        }
        Pool above = repository.parent(child);
        if (above != null) {
            throw new InvalidPoolException(child.name() + " is already a member of " + above.name() + ".");
        }
        int depth = 1;
        for (Pool p = repository.parent(pool); p != null; p = repository.parent(p)) {
            if (p.id() == child.id()) {
                throw new InvalidPoolException(pool.name() + " is already within " + child.name() + ".");
            }
            depth++;
        }
        if (depth + height(child, repository) > MAX_DEPTH) {
            throw new InvalidPoolException("A chain of pools is at most " + MAX_DEPTH + " deep.");
        }
        return child;
    }

    /** How many levels of pools this pool heads, itself included. */
    private static int height(Pool pool, PoolRepository repository) {
        int below = 0;
        for (Pool c : repository.children(pool)) {
            below = Math.max(below, height(c, repository));
        }
        return below + 1;
    }
}

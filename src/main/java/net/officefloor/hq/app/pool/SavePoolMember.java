package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A pool or person not on the sheet, or a person already in the pool, is refused. A member pool is
 * refused when it is the pool itself, is already a member of a pool, contains the pool through any
 * chain, or would make a chain of pools more than three deep.
 */
public class SavePoolMember {
    static final int MAX_DEPTH = 3;

    public void save(@Val PoolMemberInput input, @Val Sheet sheet, PoolRepository pools, PersonRepository people,
            Out<PoolMember> memberOut) throws InvalidPoolException {
        Pool pool = pools.find(sheet.id(), input.pool());
        if (pool == null) throw new InvalidPoolException("No pool " + input.pool());
        if (input.memberPool() != null) {
            Pool member = pools.find(sheet.id(), input.memberPool());
            if (member == null) throw new InvalidPoolException("No pool " + input.memberPool());
            if (member.id().equals(pool.id())) throw new InvalidPoolException("A pool cannot contain itself");
            if (pools.parentOf(member) != null) {
                throw new InvalidPoolException(member.name() + " is already a member of a pool");
            }
            int level = 1;
            for (Pool above = pools.parentOf(pool); above != null; above = pools.parentOf(above)) {
                if (above.id().equals(member.id())) throw new InvalidPoolException("A pool cannot contain itself");
                level++;
            }
            if (level + height(member, pools) > MAX_DEPTH) {
                throw new InvalidPoolException("A chain of pools is at most " + MAX_DEPTH + " deep");
            }
            pools.addMemberPool(pool, member, input.share());
            memberOut.set(new PoolMember(sheet.id(), pool.name(), null, member.name(), member.name(), input.share(),
                    false, false));
            return;
        }
        Person person = people.find(sheet.id(), input.personId());
        if (person == null) throw new InvalidPoolException("No such person");
        if (pools.isMember(pool, person.getId())) {
            throw new InvalidPoolException(person.getName() + " is already in " + pool.name());
        }
        pools.addMember(pool, person.getId(), input.share());
        memberOut.set(new PoolMember(sheet.id(), pool.name(), person.getId(), null, person.getName(), input.share(),
                person.isRetired(), person.getHolds().stream().anyMatch(h -> h.covers(sheet.today()))));
    }

    /** How many pools deep the chain below this pool goes, the pool itself counting as one. */
    private static int height(Pool pool, PoolRepository pools) {
        int below = 0;
        for (Pool m : pools.memberPools(pool)) below = Math.max(below, height(m, pools));
        return 1 + below;
    }
}

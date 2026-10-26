package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds the person, or another pool, to the pool with its share. A person already in the pool is refused; so is a pool
 * that is the pool itself or contains it through any chain, a pool already a member of a pool, and one that would make
 * a chain of pools more than {@link PoolRepository#MAX_DEPTH} deep.
 */
public class AddPoolMember {
    public void add(@Val SheetResponse sheet, @Val PoolMemberInput input, PoolRepository pools, PersonRepository people,
            Out<PoolMemberAdded> added) throws BadRequestException {
        Long poolId = pools.findId(sheet.id(), input.pool());
        if (poolId == null) {
            throw new BadRequestException("No such pool on this sheet");
        }
        if (input.personId() == null) {
            Long memberId = pools.findId(sheet.id(), input.memberPool());
            if (memberId == null) {
                throw new BadRequestException("No such pool on this sheet");
            }
            if (pools.chainUp(poolId).contains(memberId)) {
                throw new BadRequestException("A pool cannot contain itself");
            }
            if (pools.parentOf(memberId) != null) {
                throw new BadRequestException(input.memberPool() + " is already a member of a pool");
            }
            if (pools.chainUp(poolId).size() + pools.height(memberId) > PoolRepository.MAX_DEPTH) {
                throw new BadRequestException("A chain of pools is at most " + PoolRepository.MAX_DEPTH + " deep");
            }
            if (!pools.addPoolMember(sheet.id(), poolId, memberId, input.share())) {
                throw new BadRequestException(input.memberPool() + " is already a member of a pool");
            }
            added.set(new PoolMemberAdded(sheet.id(), input.pool(), input.memberPool(), input.share()));
            return;
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

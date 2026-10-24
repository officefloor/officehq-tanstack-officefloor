package net.officefloor.hq.app.pool;

import java.util.Optional;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person, or another pool, to the pool with its share. Refuses a person not on the sheet, or one already in
 *  the pool; refuses a pool not on the sheet, one already a member of a pool, one that would contain itself through
 *  any chain, or one that would make a chain of pools more than three deep. */
public class ApplyPoolMember {
    private static final int MAX_DEPTH = 3;

    public void apply(@Val ValidPoolMember request, @Val Pool pool, PersonRepository people, PoolRepository pools,
            PoolMemberRepository members, Out<PoolMember> memberOut, Out<MemberName> nameOut)
            throws InvalidPoolException {
        if (request.memberPoolId() != null) {
            Pool member = pools.findByIdAndSheetId(request.memberPoolId(), pool.getSheetId())
                    .orElseThrow(() -> new InvalidPoolException("There is no such pool on the sheet."));
            if (member.getId().equals(pool.getId())) {
                throw new InvalidPoolException("A pool cannot be a member of itself.");
            }
            if (members.findByMemberPoolId(member.getId()).isPresent()) {
                throw new InvalidPoolException("That pool is already a member of a pool.");
            }
            // Walk up from the pool: reaching the member would make it contain itself.
            int above = 1;
            for (Optional<PoolMember> up = members.findByMemberPoolId(pool.getId()); up.isPresent();
                    up = members.findByMemberPoolId(up.get().getPoolId())) {
                if (up.get().getPoolId().equals(member.getId())) {
                    throw new InvalidPoolException("A pool cannot contain itself.");
                }
                above++;
            }
            if (above + height(member.getId(), members) > MAX_DEPTH) {
                throw new InvalidPoolException("A chain of pools is at most three deep.");
            }
            nameOut.set(new MemberName(member.getName()));
            memberOut.set(members.saveAndFlush(PoolMember.ofPool(pool.getId(), member.getId(), request.share())));
            return;
        }
        Person person = people.findByIdAndSheetId(request.personId(), pool.getSheetId())
                .orElseThrow(() -> new InvalidPoolException("There is no such person on the sheet."));
        if (members.existsByPoolIdAndPersonId(pool.getId(), person.getId())) {
            throw new InvalidPoolException("That person is already in the pool.");
        }
        nameOut.set(new MemberName(person.getName()));
        memberOut.set(members.saveAndFlush(new PoolMember(pool.getId(), person.getId(), request.share())));
    }

    /** How many levels of pools the pool heads: 1 for itself, plus its deepest member pool's. */
    private static int height(Long poolId, PoolMemberRepository members) {
        int deepest = 0;
        for (PoolMember m : members.findByPoolIdOrderByIdAsc(poolId)) {
            if (m.getMemberPoolId() != null) deepest = Math.max(deepest, height(m.getMemberPoolId(), members));
        }
        return 1 + deepest;
    }

    /** The added member's name, a person's or a pool's. */
    public record MemberName(String name) {
    }
}

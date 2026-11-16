package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the member in the pool; an unknown pool or person, or a person already in the pool, is refused. A member pool
 *  is refused when it is unknown, already a member of a pool, would contain itself through any chain, or would make a
 *  chain of pools more than three deep. */
public class AddPoolMember {

    private static final int MAX_DEPTH = 3;

    public void add(@PathVariable("sheetId") String sheetId, @Val PoolMemberRequest request, PoolRepository pools,
            Out<PoolMemberValues> values) throws InvalidPoolException {
        String sheet = sheetId.trim();
        String pool = request.pool().trim();
        Long poolId = pools.idOf(sheet, pool);
        if (poolId == null) {
            throw new InvalidPoolException("No pool " + pool);
        }
        BigDecimal share = new BigDecimal(request.share().trim()).setScale(2, RoundingMode.UNNECESSARY);
        String by = request.by() == null ? "" : request.by().trim();
        if (request.personId() == null) {
            String member = request.memberPool().trim();
            Long memberId = pools.idOf(sheet, member);
            if (memberId == null) {
                throw new InvalidPoolException("No pool " + member);
            }
            if (pools.parentOf(memberId) != null) {
                throw new InvalidPoolException(member + " is already a member of a pool");
            }
            int above = 0;
            for (Long p = poolId; p != null; p = pools.parentOf(p)) {
                if (p.equals(memberId)) {
                    throw new InvalidPoolException(member + " cannot contain itself");
                }
                above++;
            }
            if (above + pools.height(memberId) > MAX_DEPTH) {
                throw new InvalidPoolException("A chain of pools is at most " + MAX_DEPTH + " deep");
            }
            pools.addPoolMember(poolId, memberId, share);
            values.set(new PoolMemberValues(pool, null, member, true, share, by));
            return;
        }
        String name = pools.personName(sheet, request.personId());
        if (name == null) {
            throw new InvalidPoolException("No such person on the sheet");
        }
        if (!pools.addMember(poolId, request.personId(), share)) {
            throw new InvalidPoolException(name + " is already in " + pool);
        }
        values.set(new PoolMemberValues(pool, request.personId(), name, false, share, by));
    }
}

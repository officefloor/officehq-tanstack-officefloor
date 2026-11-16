package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the member in the pool; an unknown pool or person, or a person already in the pool, is refused. */
public class AddPoolMember {
    public void add(@PathVariable("sheetId") String sheetId, @Val PoolMemberRequest request, PoolRepository pools,
            Out<PoolMemberValues> values) throws InvalidPoolException {
        String sheet = sheetId.trim();
        String pool = request.pool().trim();
        Long poolId = pools.idOf(sheet, pool);
        if (poolId == null) {
            throw new InvalidPoolException("No pool " + pool);
        }
        String name = pools.personName(sheet, request.personId());
        if (name == null) {
            throw new InvalidPoolException("No such person on the sheet");
        }
        BigDecimal share = new BigDecimal(request.share().trim()).setScale(2, RoundingMode.UNNECESSARY);
        if (!pools.addMember(poolId, request.personId(), share)) {
            throw new InvalidPoolException(name + " is already in " + pool);
        }
        values.set(new PoolMemberValues(pool, request.personId(), name, share,
                request.by() == null ? "" : request.by().trim()));
    }
}

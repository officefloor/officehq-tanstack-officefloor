package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a pool name that is blank, or already a pool or a crew on the sheet (its name trimmed). */
public class ValidatePool {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest request,
            Pools pools, Out<Pool> pool) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetRequestException("A pool's name cannot be blank");
        }
        if (name.length() > 200) {
            throw new InvalidSheetRequestException("A pool's name is at most 200 characters");
        }
        if (pools.has(sheetId, name)) {
            throw new InvalidSheetRequestException("Already a pool: " + name);
        }
        if (pools.hasCrew(sheetId, name)) {
            throw new InvalidSheetRequestException("Already a crew: " + name);
        }
        pool.set(new Pool(sheetId, name, request.user()));
    }
}

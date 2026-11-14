package net.officefloor.hq.app.pool;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a pool whose name is blank once trimmed, or is already a pool's or a crew's on the sheet. */
public class ValidatePoolRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid PoolRequest request, PoolRepository pools,
            Out<PoolName> pool) throws InvalidPoolException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("A pool's name is required.");
        }
        if (pools.nameTaken(sheet.id(), name)) {
            throw new InvalidPoolException("There is already a pool named " + name + ".");
        }
        if (pools.crewNamed(sheet.id(), name)) {
            throw new InvalidPoolException("There is already a crew named " + name + ", and a crew is a pool.");
        }
        pool.set(new PoolName(sheet.id(), name));
    }
}

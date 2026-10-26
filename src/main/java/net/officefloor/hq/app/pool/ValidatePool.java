package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** A pool's name (trimmed, required). */
public class ValidatePool {
    public void validate(@Val SheetResponse sheet, @RequestBody PoolRequest request, Out<Pool> pool,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Enter a pool name");
        }
        if (name.length() > 200) {
            throw new BadRequestException("A pool name is at most 200 characters");
        }
        pool.set(new Pool(sheet.id(), name));
        changedBy.set(by);
    }
}

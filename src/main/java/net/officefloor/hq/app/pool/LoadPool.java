package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's pool by id; refuses one not on the sheet. */
public class LoadPool {
    public void load(@PathVariable("poolId") long poolId, @Val Sheet sheet, PoolRepository repository,
            Out<Pool> poolOut) throws InvalidPoolException {
        poolOut.set(repository.findByIdAndSheetId(poolId, sheet.getId())
                .orElseThrow(() -> new InvalidPoolException("There is no such pool.")));
    }
}

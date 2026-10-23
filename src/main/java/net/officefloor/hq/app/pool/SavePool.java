package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the pool, refusing a name already a pool or a crew on the sheet. */
public class SavePool {
    public void save(@PathVariable("sheetId") String sheetId, @Val PoolInput input, PeriodRepository periods,
            PoolRepository repository, CrewRepository crews, Out<Pool> pool) throws InvalidPoolException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (repository.find(id, input.name()) != null) {
            throw new InvalidPoolException(input.name() + " is already a pool.");
        }
        if (crews.find(id, input.name()) != null) {
            throw new InvalidPoolException(input.name() + " is already a crew.");
        }
        pool.set(repository.add(id, input));
    }
}

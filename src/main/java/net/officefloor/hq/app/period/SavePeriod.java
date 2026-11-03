package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePeriod {
    public void save(@PathVariable("sheetId") String sheetId, @Val PeriodInput input,
            ServerHttpConnection connection, PeriodRepository repository, Audit audit, Out<Period> period)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.named(id, input.name())) {
            throw new BadRequestException("a period named " + input.name() + " is already on the sheet");
        }
        if (repository.overlaps(id, input.from(), input.to())) {
            throw new BadRequestException("the period overlaps another period");
        }
        Period saved = repository.add(id, input.name(), input.from(), input.to(), input.cap());
        audit.record("PERIOD_ADDED sheet=" + id + " by=" + by + " period=" + saved.name() + " from=" + saved.from()
                + " to=" + saved.to() + (saved.cap() == null ? "" : " cap=" + saved.cap()));
        period.set(saved);
    }
}

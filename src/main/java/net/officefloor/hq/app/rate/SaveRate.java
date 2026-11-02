package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateInput input,
            ServerHttpConnection connection, RateRepository repository, Audit audit, Out<Rate> rate)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        Rate saved = repository.set(id, input.grade(), input.pct());
        audit.record("RATE_SET sheet=" + id + " by=" + by + " grade=" + saved.grade() + " rate=" + saved.pct());
        rate.set(saved);
    }
}

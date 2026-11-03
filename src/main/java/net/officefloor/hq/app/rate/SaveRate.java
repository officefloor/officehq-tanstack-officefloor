package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateInput input,
            ServerHttpConnection connection, RateRepository repository, Audit audit, Rollup rollup, Out<Rate> rate)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        rate.set(set(id, by, input, repository, audit, rollup));
    }

    /** Set a rate on the sheet, writing RATE_SET and the rollups it changes. */
    static Rate set(String id, String by, RateInput input, RateRepository repository, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(id);
        Rate saved = repository.set(id, input.grade(), input.pct(), input.from(), input.to());
        audit.record("RATE_SET sheet=" + id + " by=" + by + " grade=" + saved.grade() + " rate=" + saved.pct()
                + (saved.from() == null ? "" : " from=" + saved.from())
                + (saved.to() == null ? "" : " to=" + saved.to()));
        rollup.record(id, by, before);
        return saved;
    }
}

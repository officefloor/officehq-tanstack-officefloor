package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Set each imported rate, in order, exactly as the rate form does (see {@link SaveRate}). */
public class ImportRates {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateImportInput input,
            ServerHttpConnection connection, RateRepository repository, Audit audit, Rollup rollup,
            Out<RateImportResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        for (RateInput rate : input.rates()) {
            SaveRate.set(id, by, rate, repository, audit, rollup);
        }
        result.set(new RateImportResult(input.rates().size()));
    }
}

package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SetImportedRates {
    // Each checked line is set in order, as the rate form sets it: replacing a rate for the same grade and from-date,
    // RATE_SET, then its ROLLUP lines.
    public void set(@PathVariable("sheetId") String sheetId, @Val ImportedRates imported, @Val Figures before,
            Rates rates, Rollup rollup, ServerHttpConnection connection, Audit audit, Out<RateImportResponse> result) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        Figures figures = before;
        for (Rate rate : imported.rates()) {
            rates.save(rate);
            new AuditRateSet().audit(rate, connection, audit);
            figures = RollupLines.record(sheet, user, figures, rollup, audit);
        }
        result.set(new RateImportResponse(imported.rates().size()));
    }
}

package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SetImportedOpenings {
    // Each checked line adds its amount to the person's opening balance for the period, in order (OPENING_SET). A
    // (soft-)closed period's carried figures stay as fixed; the balance counts once it is reopened and closed again.
    public void set(@PathVariable("sheetId") String sheetId, @Val ImportedOpenings imported, Periods periods,
            ServerHttpConnection connection, Audit audit, Out<OpeningImportResponse> result) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        for (ImportedOpenings.Line line : imported.lines()) {
            periods.addOpening(sheet, line.period().name(), line.person().id(), line.amount());
            audit.record(Audit.line("OPENING_SET", sheet, user, "name=" + line.person().name(),
                    "period=" + line.period().name(), "amount=" + line.amount().toPlainString()));
        }
        result.set(new OpeningImportResponse(imported.lines().size()));
    }
}

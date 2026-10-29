package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AddImportedPeople {
    // Each checked line is added in order as the person form adds one: PERSON_ADDED, then its ROLLUP lines.
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, @Val Figures before,
            People people, Rollup rollup, ServerHttpConnection connection, Audit audit, Out<ImportResponse> result) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        Figures figures = before;
        for (PersonRequest request : imported.people()) {
            new AuditPersonAdded().audit(people.add(sheet, request), connection, audit);
            figures = RollupLines.record(sheet, user, figures, rollup, audit);
        }
        result.set(new ImportResponse(imported.people().size()));
    }
}

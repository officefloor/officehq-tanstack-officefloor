package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Snapshot;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AuditSnapshots {
    // Once the sheet's today is changed: one SNAPSHOT per person, in list order, with the due-total as it stood on the
    // day being left (the figures taken before the change), before any period closes and before the ROLLUP lines. Each
    // is about the day being left, so names the steward who delegated to the user for that day. Each is kept on the
    // person too, for their snapshot lines.
    public void audit(@PathVariable("sheetId") String sheetId, @Val Figures before, @Val TodayLeft left,
            ServerHttpConnection connection, Audit audit, People people) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        before.people().forEach((id, person) -> {
            people.snapshot(id, new Snapshot(left.today(), person.dueTotal()));
            audit.record(Audit.line("SNAPSHOT", sheet, user, "name=" + person.name(), "due-total=" + person.dueTotal()),
                    left.today());
        });
    }
}

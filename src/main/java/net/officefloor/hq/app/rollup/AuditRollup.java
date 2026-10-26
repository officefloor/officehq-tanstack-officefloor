package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AuditRollup {
    // After the change's own lines: one ROLLUP per person whose figures moved (a new person counts as moved),
    // in the order they were added, then TOTALS when a sheet total moved.
    public void audit(@PathVariable("sheetId") String sheetId, @Val Figures before, Rollup rollup,
            ServerHttpConnection connection, Audit audit) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        Figures after = rollup.figures(sheet);
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                audit.record(Audit.line("ROLLUP", sheet, user, "person=" + now.name(), "claimed=" + now.claimed(),
                        "due-total=" + now.dueTotal()));
            }
        });
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())) {
            audit.record(Audit.line("TOTALS", sheet, user, "claimed-total=" + after.claimedTotal(),
                    "due-total=" + after.dueTotal()));
        }
    }
}

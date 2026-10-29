package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AuditScores {
    // Once the sheet's today is changed, while the score log is on: one SCORE per person, in list order, with the score
    // as it stood on the day being left (the figures taken before the change), before the SNAPSHOT lines.
    public void audit(@PathVariable("sheetId") String sheetId, @Val Figures before, @Val TodayLeft left,
            ServerHttpConnection connection, Audit audit, Settings settings) {
        String sheet = sheetId.trim();
        if (!settings.scoreLog(sheet)) {
            return;
        }
        String user = Audit.user(connection);
        before.people().values().forEach(person -> audit.record(
                Audit.line("SCORE", sheet, user, "name=" + person.name(), "score=" + person.score()), left.today()));
    }
}

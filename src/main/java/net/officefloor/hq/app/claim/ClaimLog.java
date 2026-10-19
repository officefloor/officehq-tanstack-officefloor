package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Each claim's status log: one line per status it has had, oldest first, as
 *  {@code <STATUS> by <user> on <date> due=<due>} — the date the sheet's today when the change was made,
 *  the due the claim's figure as the screen showed it at that moment (see {@link Rollup#figureOf}). */
@Service
public class ClaimLog {

    private final JdbcTemplate jdbc;
    private final SheetClock clock;
    private final Rollup rollup;

    public ClaimLog(JdbcTemplate jdbc, SheetClock clock, Rollup rollup) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.rollup = rollup;
    }

    /** Logs the claim's status as it is now, once the change is saved. */
    public void record(Claim claim, String user) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), claim.getStatus().name(), user,
                Date.valueOf(clock.today(claim.getSheetId())), rollup.figureOf(claim));
    }

    /** The lines of every claim on the sheet, by claim id, oldest first. */
    public Map<Long, List<String>> bySheet(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getDate("log_date").toLocalDate() + " due=" + rs.getString("due"));
        }, sheetId);
        return lines;
    }

    /** The claim's lines, oldest first. */
    public List<String> of(Claim claim) {
        return bySheet(claim.getSheetId()).getOrDefault(claim.getId(), List.of());
    }
}

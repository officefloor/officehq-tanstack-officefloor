package net.officefloor.hq.app.period;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rollovers made on each sheet, each with the periods it generated and the opening balances it set, so the last
 *  one can be undone. */
@Repository
public class RolloverRepository {

    /** A rollover as kept: the old fiscal year, who made it, the sheet's today it was made on, and what it made. */
    public record Rollover(long id, String sheetId, String fy, String by, LocalDate madeOn, List<Long> periodIds,
            List<Long> openingIds, boolean undone) {
    }

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public RolloverRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    private static String idsText(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> parseIds(String text) {
        return text == null || text.isEmpty() ? List.of() : Arrays.stream(text.split(",")).map(Long::parseLong).toList();
    }

    /** Keeps a rollover, made on the sheet's today. */
    public void add(String sheetId, String fy, String by, List<Long> periodIds, List<Long> openingIds) {
        jdbc.update("INSERT INTO rollover (sheet_id, fy, made_by, made_on, period_ids, opening_ids) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, fy, by, Date.valueOf(sheets.today(sheetId)), idsText(periodIds), idsText(openingIds));
    }

    /** The sheet's last rollover, or null when it has none. */
    public Rollover last(String sheetId) {
        List<Rollover> found = jdbc.query("SELECT id, sheet_id, fy, made_by, made_on, period_ids, opening_ids, undone FROM rollover"
                + " WHERE sheet_id = ? ORDER BY id DESC LIMIT 1", (ResultSet rs, int n) -> new Rollover(rs.getLong("id"),
                        rs.getString("sheet_id"), rs.getString("fy"), rs.getString("made_by"),
                        rs.getObject("made_on", LocalDate.class), parseIds(rs.getString("period_ids")),
                        parseIds(rs.getString("opening_ids")), rs.getBoolean("undone")), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether any claim of the sheet, whatever its status, is dated inside the period. */
    public boolean hasClaims(Period period) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND claim_date >= ? AND claim_date <= ?",
                Integer.class, period.sheetId(), Date.valueOf(period.from()), Date.valueOf(period.to()));
        return n != null && n > 0;
    }

    /** Whether the period has an opening balance other than the ones listed (one pasted on it by hand). */
    public boolean hasOtherOpenings(Period period, List<Long> openingIds) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM opening WHERE sheet_id = ? AND period_id = ?", Long.class,
                period.sheetId(), period.id());
        return !openingIds.containsAll(ids);
    }

    /** Undoes the rollover: removes the openings it set and the periods it generated (with what was kept about them), and
     *  marks it undone; false when it was already undone. */
    public boolean undo(Rollover rollover) {
        if (jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ? AND undone = FALSE", rollover.id()) != 1) {
            return false;
        }
        for (long id : rollover.openingIds()) {
            jdbc.update("DELETE FROM opening WHERE sheet_id = ? AND id = ?", rollover.sheetId(), id);
        }
        for (long id : rollover.periodIds()) {
            jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_id = ?", rollover.sheetId(), id);
            jdbc.update("DELETE FROM period_log WHERE sheet_id = ? AND period_id = ?", rollover.sheetId(), id);
            jdbc.update("DELETE FROM period WHERE sheet_id = ? AND id = ?", rollover.sheetId(), id);
        }
        return true;
    }
}

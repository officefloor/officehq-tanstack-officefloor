package net.officefloor.hq.app.rollover;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The rollovers made on each sheet, with the periods each generated and the opening balances each set. */
@Repository
public class RolloverRepository {

    /** A rollover kept on a sheet. */
    public record Rollover(long id, String sheetId, String fy, String madeBy, LocalDate madeOn, boolean undone,
            List<String> periodNames, List<Long> openingIds) {
    }

    private final JdbcTemplate jdbc;

    public RolloverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a rollover just made. */
    public void insert(String sheetId, String fy, String madeBy, LocalDate madeOn, List<String> periodNames,
            List<Long> openingIds) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO rollover (sheet_id, fy, made_by, made_on) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, fy);
            ps.setString(3, madeBy);
            ps.setObject(4, madeOn);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        periodNames.forEach(name -> jdbc.update("INSERT INTO rollover_period (rollover_id, period_name) VALUES (?, ?)",
                id, name));
        openingIds.forEach(openingId -> jdbc.update(
                "INSERT INTO rollover_opening (rollover_id, opening_id) VALUES (?, ?)", id, openingId));
    }

    /** Whether the old fiscal year has been rolled over on the sheet and that rollover has not been undone. */
    public boolean standing(String sheetId, String fy) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rollover WHERE sheet_id = ? AND fy = ? AND NOT undone",
                Integer.class, sheetId, fy) > 0;
    }

    /** The last rollover made on the sheet (undone or not), or null when none was. */
    public Rollover last(String sheetId) {
        return jdbc.query("SELECT * FROM rollover WHERE sheet_id = ? ORDER BY id DESC LIMIT 1", (rs, i) -> {
            long id = rs.getLong("id");
            return new Rollover(id, rs.getString("sheet_id"), rs.getString("fy"), rs.getString("made_by"),
                    rs.getObject("made_on", LocalDate.class), rs.getBoolean("undone"),
                    jdbc.queryForList("SELECT period_name FROM rollover_period WHERE rollover_id = ?", String.class, id),
                    jdbc.queryForList("SELECT opening_id FROM rollover_opening WHERE rollover_id = ?", Long.class, id));
        }, sheetId).stream().findFirst().orElse(null);
    }

    /** Mark the rollover undone: false when it already was, and nothing is written. */
    public boolean markUndone(long id) {
        return jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ? AND NOT undone", id) == 1;
    }
}

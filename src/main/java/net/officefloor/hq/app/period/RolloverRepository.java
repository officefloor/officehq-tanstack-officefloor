package net.officefloor.hq.app.period;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The rollovers store: each rollover made on a sheet, with what it generated and set. */
@Repository
public class RolloverRepository {

    private final JdbcTemplate jdbc;

    public RolloverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a rollover made now, with the periods it generated and the openings it added. */
    public void insert(String sheetId, String fy, String by, LocalDate on, List<String> periods,
            List<Rollover.Opening> openings) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update((c) -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO rollover (sheet_id, fy, made_by, made_on)"
                    + " VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, fy);
            ps.setString(3, by);
            ps.setObject(4, on);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        for (String name : periods) {
            jdbc.update("INSERT INTO rollover_period (rollover_id, period_name) VALUES (?, ?)", id, name);
        }
        for (Rollover.Opening o : openings) {
            jdbc.update("INSERT INTO rollover_opening (rollover_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                    id, o.periodName(), o.personId(), o.amount());
        }
    }

    /** Whether a rollover of the fiscal year stands on the sheet: made, and not undone. */
    public boolean stands(String sheetId, String fy) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rollover WHERE sheet_id = ? AND fy = ? AND NOT undone",
                Integer.class, sheetId, fy);
        return n != null && n > 0;
    }

    /** The sheet's last rollover, or null when none was ever made. */
    public Rollover last(String sheetId) {
        List<Rollover> found = jdbc.query("SELECT id, sheet_id, fy, made_by, made_on, undone FROM rollover"
                + " WHERE sheet_id = ? ORDER BY id DESC LIMIT 1", (rs, n) -> {
                    long id = rs.getLong("id");
                    return new Rollover(id, rs.getString("sheet_id"), rs.getString("fy"), rs.getString("made_by"),
                            rs.getObject("made_on", LocalDate.class), rs.getBoolean("undone"),
                            jdbc.queryForList("SELECT period_name FROM rollover_period WHERE rollover_id = ?",
                                    String.class, id),
                            jdbc.query("SELECT period_name, person_id, amount FROM rollover_opening WHERE rollover_id = ?",
                                    (o, m) -> new Rollover.Opening(o.getString("period_name"), o.getLong("person_id"),
                                            o.getBigDecimal("amount")), id));
                }, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Mark the rollover undone: false when it already was, as when another request undid it first. */
    public boolean markUndone(Rollover rollover) {
        return jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ? AND NOT undone", rollover.id()) == 1;
    }
}

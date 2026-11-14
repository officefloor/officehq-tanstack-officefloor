package net.officefloor.hq.app.rollover;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RolloverRepository {

    private final JdbcTemplate jdbc;

    public RolloverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Record a rollover and the periods it generated; its id. */
    public long insert(String sheetId, String fy, String madeBy, LocalDate madeOn, List<String> periodNames) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO rollover (sheet_id, fy, made_by, made_on) VALUES (?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, fy);
            ps.setString(3, madeBy);
            ps.setDate(4, Date.valueOf(madeOn));
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        periodNames.forEach((name) -> jdbc.update(
                "INSERT INTO rollover_period (rollover_id, period_name) VALUES (?, ?)", id, name));
        return id;
    }

    /** The sheet's latest rollover, or null when none was made. */
    public RolloverRecord last(String sheetId) {
        List<RolloverRecord> found = jdbc.query(
                "SELECT id, sheet_id, fy, made_by, made_on, undone FROM rollover WHERE sheet_id = ? ORDER BY id DESC LIMIT 1",
                (rs, n) -> new RolloverRecord(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("fy"),
                        rs.getString("made_by"), rs.getObject("made_on", LocalDate.class), rs.getBoolean("undone"),
                        List.of()),
                sheetId);
        if (found.isEmpty()) {
            return null;
        }
        RolloverRecord r = found.get(0);
        List<String> names = jdbc.queryForList("SELECT period_name FROM rollover_period WHERE rollover_id = ?",
                String.class, r.id());
        return new RolloverRecord(r.id(), r.sheetId(), r.fy(), r.madeBy(), r.madeOn(), r.undone(), names);
    }

    /** Take the rollover back: remove the periods it generated and the opening balances it set, and mark it undone. */
    public void undo(RolloverRecord rollover) {
        String sheetId = rollover.sheetId();
        jdbc.update("DELETE FROM opening_balance WHERE sheet_id = ? AND rollover_id = ?", sheetId, rollover.id());
        for (String name : rollover.periodNames()) {
            jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
            jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, name);
            jdbc.update("DELETE FROM period WHERE sheet_id = ? AND name = ?", sheetId, name);
        }
        jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ?", rollover.id());
    }
}

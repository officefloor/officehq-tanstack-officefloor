package net.officefloor.hq.app.period;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The rollover store: each year end rollover on a sheet, so the last one can be undone. */
@Repository
public class Rollovers {

    /** A rollover as made: its id, who made it on which of the sheet's todays, the old fiscal year (FY<yyyy>), the
     *  periods it generated, and whether it has been undone. */
    public record Rollover(long id, String user, LocalDate madeOn, String fiscalYear, List<String> periods,
            boolean undone) {
    }

    private final JdbcTemplate jdbc;

    public Rollovers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Records a rollover of the fiscal year by the user, made on the sheet's today: its id. */
    public long start(String sheetId, String user, String fiscalYear) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement insert = connection.prepareStatement("INSERT INTO rollover (sheet_id, made_by, made_on,"
                    + " fiscal_year) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                    Statement.RETURN_GENERATED_KEYS);
            insert.setString(1, sheetId);
            insert.setString(2, user);
            insert.setString(3, sheetId);
            insert.setString(4, fiscalYear);
            return insert;
        }, key);
        return key.getKey().longValue();
    }

    /** Whether a rollover of the fiscal year made before the given one still stands (was not undone). */
    public boolean standsBefore(String sheetId, String fiscalYear, long id) {
        Integer standing = jdbc.queryForObject("SELECT COUNT(*) FROM rollover WHERE sheet_id = ? AND fiscal_year = ?"
                + " AND id < ? AND NOT undone", Integer.class, sheetId, fiscalYear, id);
        return standing != null && standing > 0;
    }

    /** Records the names of the periods the rollover generated. */
    public void generated(long id, List<String> periods) {
        jdbc.update("UPDATE rollover SET periods = ? WHERE id = ?", String.join(",", periods), id);
    }

    /** The sheet's last rollover, its sheet's row locked until the transaction ends (so it is undone at most
     *  once); null when the sheet has none. */
    public Rollover last(String sheetId) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), sheetId);
        List<Rollover> found = jdbc.query("SELECT * FROM rollover WHERE sheet_id = ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> {
                    String periods = rs.getString("periods");
                    return new Rollover(rs.getLong("id"), rs.getString("made_by"), rs.getDate("made_on").toLocalDate(),
                            rs.getString("fiscal_year"), periods.isEmpty() ? List.of() : Arrays.asList(periods.split(",")),
                            rs.getBoolean("undone"));
                }, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Marks the rollover undone. */
    public void undone(long id) {
        jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ?", id);
    }
}

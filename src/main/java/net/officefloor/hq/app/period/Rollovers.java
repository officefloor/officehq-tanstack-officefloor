package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The last rollover of each sheet, kept so it can be undone. */
@Repository
public class Rollovers {

    /** A rollover: the fiscal year it rolled over, who made it on which sheet today, the periods it
     *  generated, and the amount it added to each person's opening balance (by period name, then
     *  person id). */
    public record Made(String sheetId, int fy, String user, LocalDate madeOn, List<String> periods,
            Map<String, Map<Long, BigDecimal>> openings) {
    }

    private final JdbcTemplate jdbc;

    public Rollovers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's last rollover, or null when there is none to undo. Locks its row for the rest of
     *  the transaction. */
    public Made last(String sheetId) {
        Made made = jdbc.query("SELECT fy, user_name, made_on, periods FROM rollover WHERE sheet_id = ? FOR UPDATE",
                (rs, i) -> new Made(sheetId, rs.getInt("fy"), rs.getString("user_name"),
                        rs.getDate("made_on").toLocalDate(),
                        rs.getString("periods").isEmpty() ? List.of() : Arrays.asList(rs.getString("periods").split(",")),
                        new LinkedHashMap<>()), sheetId).stream().findFirst().orElse(null);
        if (made == null) return null;
        jdbc.query("SELECT period_name, person_id, amount FROM rollover_opening WHERE sheet_id = ? ORDER BY period_name, person_id",
                rs -> {
                    made.openings().computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
                }, sheetId);
        return made;
    }

    /** Keep this as the sheet's last rollover, in place of the one before. */
    public void save(Made made) {
        forget(made.sheetId());
        jdbc.update("INSERT INTO rollover (sheet_id, fy, user_name, made_on, periods) VALUES (?, ?, ?, ?, ?)",
                made.sheetId(), made.fy(), made.user(), Date.valueOf(made.madeOn()), String.join(",", made.periods()));
        made.openings().forEach((period, amounts) -> amounts.forEach((personId, amount) -> jdbc.update(
                "INSERT INTO rollover_opening (sheet_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                made.sheetId(), period, personId, amount)));
    }

    /** The sheet has no rollover to undo. */
    public void forget(String sheetId) {
        jdbc.update("DELETE FROM rollover_opening WHERE sheet_id = ?", sheetId);
        jdbc.update("DELETE FROM rollover WHERE sheet_id = ?", sheetId);
    }
}

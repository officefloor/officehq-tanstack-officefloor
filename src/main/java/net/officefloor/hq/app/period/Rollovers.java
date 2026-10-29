package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The store of the rollovers made on each sheet, so the last one can be undone. */
@Repository
public class Rollovers {

    private final JdbcTemplate jdbc;

    public Rollovers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Records a rollover with the periods it generated and the opening balances it set. */
    public void add(String sheetId, String fy, String madeBy, LocalDate madeOn, List<String> periods,
            Map<String, Map<Long, BigDecimal>> openings) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rollover (sheet_id, fy, made_by, made_on) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, fy);
            ps.setString(3, madeBy);
            ps.setDate(4, Date.valueOf(madeOn));
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        periods.forEach(name -> jdbc.update("INSERT INTO rollover_period (rollover_id, period_name) VALUES (?, ?)", id, name));
        openings.forEach((period, byPerson) -> byPerson.forEach((personId, amount) -> jdbc.update(
                "INSERT INTO rollover_opening (rollover_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                id, period, personId, amount)));
    }

    /** Whether the sheet's fiscal year was rolled over already (and that rollover not undone). */
    public boolean rolled(String sheetId, String fy) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rollover WHERE sheet_id = ? AND fy = ? AND NOT undone",
                Integer.class, sheetId, fy);
        return n != null && n > 0;
    }

    /** The sheet's last rollover, or null when none was ever made. */
    public MadeRollover last(String sheetId) {
        MadeRollover made = jdbc.query("SELECT * FROM rollover WHERE sheet_id = ? ORDER BY id DESC LIMIT 1",
                (rs, n) -> new MadeRollover(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("fy"),
                        rs.getString("made_by"), rs.getDate("made_on").toLocalDate(), rs.getBoolean("undone"),
                        List.of(), Map.of()), sheetId).stream().findFirst().orElse(null);
        if (made == null) {
            return null;
        }
        List<String> periods = jdbc.queryForList(
                "SELECT period_name FROM rollover_period WHERE rollover_id = ? ORDER BY period_name", String.class, made.id());
        Map<String, Map<Long, BigDecimal>> openings = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM rollover_opening WHERE rollover_id = ? ORDER BY period_name, person_id", rs -> {
            openings.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
        }, made.id());
        return new MadeRollover(made.id(), made.sheetId(), made.fy(), made.madeBy(), made.madeOn(), made.undone(),
                periods, openings);
    }

    /** Marks the rollover undone; false when it already was (someone else undid it). */
    public boolean undo(long id) {
        return jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ? AND NOT undone", id) == 1;
    }
}

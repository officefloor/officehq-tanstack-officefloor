package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The claims kept per sheet, each belonging to a person on that sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person"),
            rs.getBigDecimal("amount").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("claimed_on").toLocalDate().toString(), rs.getString("note"));

    private static final String COLUMNS = "SELECT id, person, amount, claimed_on, note FROM claim";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every claim on the sheet, newest first. */
    public List<Claim> list(String sheet) {
        return jdbc.query(COLUMNS + " WHERE sheet = ? ORDER BY id DESC", ROW, sheet);
    }

    public Claim find(String sheet, long id) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE sheet = ? AND id = ?", ROW, sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public void add(String sheet, long person, ClaimInput input) {
        jdbc.update("INSERT INTO claim (sheet, person, amount, claimed_on, note) VALUES (?, ?, ?, ?, ?)", sheet, person,
                input.amount(), Date.valueOf(input.date()), input.note());
    }

    public void update(String sheet, long id, ClaimInput input) {
        jdbc.update("UPDATE claim SET amount = ?, claimed_on = ?, note = ? WHERE sheet = ? AND id = ?", input.amount(),
                Date.valueOf(input.date()), input.note(), sheet, id);
    }
}

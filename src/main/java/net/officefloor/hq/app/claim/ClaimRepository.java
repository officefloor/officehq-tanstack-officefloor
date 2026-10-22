package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Claims by sheet, newest first. */
@Repository
public class ClaimRepository {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
            rs.getString("note"));

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    /** The claim on this sheet, or null when absent. */
    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    public Claim insert(Claim claim) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setObject(4, claim.date());
            ps.setString(5, claim.note());
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(),
                claim.date(), claim.note());
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ? WHERE sheet_id = ? AND id = ?",
                claim.amount(), claim.date(), claim.note(), claim.sheetId(), claim.id());
    }
}

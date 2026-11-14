package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status";

    private static final RowMapper<Claim> ROW = (rs, n) -> new Claim(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getLong("person_id"),
            rs.getBigDecimal("amount").setScale(2).toPlainString(),
            rs.getObject("claim_date", LocalDate.class).toString(), rs.getString("note"),
            rs.getString("status"));

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC",
                ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            String user) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, created_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setObject(4, date);
            ps.setString(5, note);
            ps.setString(6, user);
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), sheetId, personId, amount.toPlainString(),
                date.toString(), note, ClaimStatus.DRAFT.name());
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ? WHERE sheet_id = ? AND id = ?",
                new BigDecimal(claim.getAmount()), LocalDate.parse(claim.getDate()), claim.getNote(),
                claim.getStatus(),
                claim.getSheetId(), claim.getId());
    }
}

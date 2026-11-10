package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims store, kept per sheet. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status";

    private static final RowMapper<Claim> ROW = (rs, n) -> new Claim(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getLong("person_id"), rs.getBigDecimal("amount"),
            rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
            ClaimStatus.valueOf(rs.getString("status")));

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC",
                ROW, sheetId);
    }

    /** The claim with this id on the sheet, or null when there is none. */
    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query(
                "SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert a new claim (assigning its id) or update an existing one. */
    public void save(Claim claim) {
        if (claim.getId() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ? WHERE id = ? AND sheet_id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(),
                    claim.getStatus().name(), claim.getId(),
                    claim.getSheetId());
            return;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, claim.getSheetId());
            ps.setLong(2, claim.getPersonId());
            ps.setBigDecimal(3, claim.getAmount());
            ps.setDate(4, Date.valueOf(claim.getDate()));
            ps.setString(5, claim.getNote());
            ps.setString(6, claim.getStatus().name());
            return ps;
        }, keys);
        claim.setId(keys.getKey().longValue());
    }
}

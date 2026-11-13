package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, due, opened_by";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC",
                ClaimRepository::map, sheetId);
    }

    /** The claim with this id on this sheet, or null. */
    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ClaimRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert a new claim (null id) or update an existing one; the claim as stored. */
    public Claim save(Claim claim) {
        if (claim.id() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, due = ?, opened_by = ? WHERE id = ? AND sheet_id = ?",
                    claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.status().name(), claim.number(),
                    claim.due(), claim.openedBy(), claim.id(), claim.sheetId());
            return claim;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setDate(4, Date.valueOf(claim.date()));
            ps.setString(5, claim.note());
            ps.setString(6, claim.status().name());
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy());
    }

    private static Claim map(ResultSet rs, int n) throws SQLException {
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                ClaimStatus.valueOf(rs.getString("status")), rs.getObject("claim_number", Integer.class),
                rs.getBigDecimal("due"), rs.getString("opened_by"));
    }
}

package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims of each sheet, newest first. */
@Repository
public class Claims {

    private static final String COLUMNS = "SELECT id, sheet_id, person_id, amount, claim_date, note, status, number, due FROM claim";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> onSheet(String sheetId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY id DESC", Claims::claim, sheetId);
    }

    /** The claim with this id on this sheet, or null when there is none. Locks the claim's row for
     *  the rest of the transaction, so two changes to one claim at once happen one after the other. */
    public Claim find(String sheetId, long id) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ? FOR UPDATE", Claims::claim, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** Insert a new claim (assigning its id) or update an existing one. */
    public void save(Claim claim) {
        if (claim.getId() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, due = ? WHERE id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                    claim.getNumber(), claim.getDue(), claim.getId());
            return;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
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

    private static Claim claim(ResultSet rs, int i) throws SQLException {
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                ClaimStatus.valueOf(rs.getString("status")), rs.getObject("number", Long.class),
                rs.getBigDecimal("due"));
    }
}

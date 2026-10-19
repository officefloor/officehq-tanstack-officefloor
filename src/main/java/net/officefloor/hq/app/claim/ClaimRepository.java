package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims of each sheet, newest first. */
@Repository
public class ClaimRepository {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(),
            rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
            rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
            rs.getString("corrects"), rs.getDate("due_by").toLocalDate());

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    public List<Claim> findByPerson(String sheetId, long personId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC", ROW, sheetId, personId);
    }

    public Claim findById(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim create(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] {"id"});
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects, Types.VARCHAR);
            ps.setDate(7, Date.valueOf(dueBy));
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null, corrects, dueBy);
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?,"
                + " stated = ?, stated_due = ?, corrects = ? WHERE sheet_id = ? AND id = ?",
                claim.getPersonId(), claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.isStated(), claim.getStatedDue(), claim.getCorrects(), claim.getSheetId(), claim.getId());
    }

    /** Gives out the sheet's next claim number. The counter is raised and read in one statement, so
     *  claims opened at the same moment never share a number, and the number is never given again. */
    public int nextNumber(String sheetId) {
        jdbc.update("MERGE INTO sheet (id) KEY (id) VALUES (?)", sheetId);
        return jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE "
                + "(UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)", Integer.class, sheetId);
    }

    /** Gives out the next number of one of the sheet's period sequences ({@code seq}), raised and read in
     *  one statement like {@link #nextNumber}; the sheet-wide sequence is left as it is. */
    public int nextInSequence(String sheetId, String seq) {
        jdbc.update("INSERT INTO claim_number_seq (sheet_id, seq, last_n) "
                + "SELECT ?, ?, 0 FROM DUAL WHERE NOT EXISTS "
                + "(SELECT 1 FROM claim_number_seq WHERE sheet_id = ? AND seq = ?)", sheetId, seq, sheetId, seq);
        return jdbc.queryForObject("SELECT last_n FROM FINAL TABLE "
                + "(UPDATE claim_number_seq SET last_n = last_n + 1 WHERE sheet_id = ? AND seq = ?)",
                Integer.class, sheetId, seq);
    }
}

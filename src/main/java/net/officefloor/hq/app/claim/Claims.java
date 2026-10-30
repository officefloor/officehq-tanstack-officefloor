package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claim store, kept per sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, i) -> {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getObject("claim_number", Long.class), rs.getBigDecimal("settled_due"), rs.getString("opened_by"));
        claim.setStated(rs.getBoolean("stated"));
        claim.setStatedDue(rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getObject("corrects", Long.class));
        java.sql.Date corrected = rs.getDate("corrected_date");
        claim.setCorrectedDate(corrected == null ? null : corrected.toLocalDate());
        return claim;
    };

    /** A claim with the date of the claim it corrects (same sheet, same person, by number). */
    private static final String SELECT = "SELECT c.*, k.claim_date AS corrected_date FROM claim c LEFT JOIN claim k"
            + " ON k.sheet_id = c.sheet_id AND k.person_id = c.person_id AND k.claim_number = c.corrects";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query(SELECT + " WHERE c.sheet_id = ? ORDER BY c.id DESC", ROW, sheetId);
    }

    /** A person's claims on the sheet, newest first. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.person_id = ? ORDER BY c.id DESC", ROW, sheetId, personId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note) {
        return add(sheetId, personId, amount, date, note, null);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, Long corrects) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects);
            return ps;
        }, keys);
        Claim claim = new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null);
        claim.setCorrects(corrects);
        claim.setCorrectedDate(correctedDate(sheetId, personId, corrects));
        return claim;
    }

    /** The date of the person's claim with this number; null when there is none (or no number). */
    public LocalDate correctedDate(String sheetId, long personId, Long number) {
        if (number == null) return null;
        List<LocalDate> found = jdbc.query("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Bind the claim to the figure a statement gives it (stated), or leave it unbound. */
    public void bind(Claim claim) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                claim.isStated(), claim.getStatedDue(), claim.getSheetId(), claim.getId());
    }

    /** Whether the person has a SETTLED claim with this number on the sheet: one a correction may correct. */
    public boolean isSettled(String sheetId, long personId, long number) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_number = ? AND status = ?", Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name());
        return found != null && found > 0;
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, corrects = ? WHERE sheet_id = ? AND id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getCorrects(),
                claim.getSheetId(), claim.getId());
    }
}

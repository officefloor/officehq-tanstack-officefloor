package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"), rs.getString("opened_by"));
        claim.setStated(rs.getBoolean("stated"));
        claim.setStatedDue(rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getString("corrects"));
        java.sql.Date corrected = rs.getDate("corrected_date");
        claim.setCorrectedDate(corrected == null ? null : corrected.toLocalDate());
        java.sql.Date dueBy = rs.getDate("due_by");
        claim.setDueBy(dueBy == null ? null : dueBy.toLocalDate());
        return claim;
    };

    /** A claim with the date of the claim it corrects (same sheet, same person, by number). */
    private static final String SELECT = "SELECT c.*, k.claim_date AS corrected_date FROM claim c LEFT JOIN claim k"
            + " ON k.sheet_id = c.sheet_id AND k.person_id = c.person_id AND k.claim_number = c.corrects";

    /** The listed_seq after every claim's: a new or moved claim is listed first. */
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(listed_seq), 0) + 1 FROM claim)";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return withLogs(sheetId, jdbc.query(SELECT + " WHERE c.sheet_id = ? ORDER BY c.listed_seq DESC, c.id DESC", ROW, sheetId));
    }

    /** The claims with their status logs (see {@link ClaimLog}), oldest line first. */
    private List<Claim> withLogs(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> logs = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, log_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            logs.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getDate("log_date").toLocalDate() + " due=" + rs.getString("due"));
        }, sheetId);
        claims.forEach(claim -> claim.setLog(logs.getOrDefault(claim.getId(), List.of())));
        return claims;
    }

    /** A person's claims on the sheet, newest first. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.person_id = ? ORDER BY c.listed_seq DESC, c.id DESC", ROW, sheetId, personId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, LocalDate dueBy) {
        return add(sheetId, personId, amount, date, note, null, dueBy);
    }

    /** Adds a DRAFT claim with its due-by fixed as given. */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, listed_seq) VALUES (?, ?, ?, ?, ?, ?, ?, "
                            + NEXT_LISTED + ")",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            return ps;
        }, keys);
        Claim claim = new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null);
        claim.setCorrects(corrects);
        claim.setDueBy(dueBy);
        claim.setCorrectedDate(correctedDate(sheetId, personId, corrects));
        return claim;
    }

    /** The date of the person's claim with this number; null when there is none (or no number). */
    public LocalDate correctedDate(String sheetId, long personId, String number) {
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
    public boolean isSettled(String sheetId, long personId, String number) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_number = ? AND status = ?", Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name());
        return found != null && found > 0;
    }

    /** Moves the claim to its (new) person, listed as their newest claim, and leaves it unbound. */
    public void reassign(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = ?, stated_due = ?, listed_seq = " + NEXT_LISTED
                + " WHERE sheet_id = ? AND id = ?", claim.getPersonId(), claim.isStated(), claim.getStatedDue(),
                claim.getSheetId(), claim.getId());
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, corrects = ? WHERE sheet_id = ? AND id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getCorrects(),
                claim.getSheetId(), claim.getId());
    }
}

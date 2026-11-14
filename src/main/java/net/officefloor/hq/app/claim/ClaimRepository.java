package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, number, settled_due, opened_by, stated, stated_due, corrects, due_by, "
            // The date it rounds by: a correction's is the date of the claim it corrects.
            + "COALESCE((SELECT k.claim_date FROM claim k WHERE k.sheet_id = claim.sheet_id AND k.number = claim.corrects), "
            + "claim_date) AS round_date";

    private static final RowMapper<Claim> ROW = (rs, n) -> withCorrects(new Claim(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getLong("person_id"),
            rs.getBigDecimal("amount").setScale(2).toPlainString(),
            rs.getObject("claim_date", LocalDate.class).toString(), rs.getString("note"),
            rs.getString("status"), rs.getString("number"),
            rs.getBigDecimal("settled_due") == null ? null
                    : rs.getBigDecimal("settled_due").setScale(2).toPlainString(),
            rs.getString("opened_by"), rs.getBoolean("stated"),
            rs.getBigDecimal("stated_due") == null ? null
                    : rs.getBigDecimal("stated_due").setScale(2).toPlainString()), rs.getString("corrects"),
            rs.getObject("round_date", LocalDate.class).toString(),
            rs.getObject("due_by", LocalDate.class).toString());

    private static Claim withCorrects(Claim claim, String corrects, String roundDate, String dueBy) {
        claim.setCorrects(corrects);
        claim.setDueBy(dueBy);
        claim.setRoundDate(roundDate);
        return claim;
    }

    /** The next place in a sheet's list (newest first); takes the sheet id as its parameter. */
    private static final String NEXT_SEQ = "(SELECT COALESCE(MAX(k.list_seq), 0) + 1 FROM claim k WHERE k.sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY list_seq DESC, id DESC",
                ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
    }

    /** The claim on the sheet with that number; null when none has it. */
    public Claim findByNumber(String sheetId, String number) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND number = ?",
                ROW, sheetId, number).stream().findFirst().orElse(null);
    }

    /** Add a claim with its due-by, fixed now. */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            String corrects, LocalDate dueBy, String user) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, created_by, corrects, due_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setObject(4, date);
            ps.setString(5, note);
            ps.setString(6, user);
            ps.setObject(7, corrects);
            ps.setObject(8, dueBy);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbc.update("UPDATE claim SET list_seq = " + NEXT_SEQ + " WHERE sheet_id = ? AND id = ?", sheetId, sheetId, id);
        return find(sheetId, id);
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, settled_due = ?, opened_by = ?, corrects = ? WHERE sheet_id = ? AND id = ?",
                new BigDecimal(claim.getAmount()), LocalDate.parse(claim.getDate()), claim.getNote(),
                claim.getStatus(), claim.getNumber(),
                claim.getSettledDue() == null ? null : new BigDecimal(claim.getSettledDue()),
                claim.getOpenedBy(), claim.getCorrects(),
                claim.getSheetId(), claim.getId());
    }

    /** Save a claim moved to another person: its person, its binding (dropped), and its place in the
     *  list, now the newest. */
    public void reassign(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = ?, stated_due = ?, list_seq = " + NEXT_SEQ
                + " WHERE sheet_id = ? AND id = ?",
                claim.getPersonId(), claim.isStated(),
                claim.getStatedDue() == null ? null : new BigDecimal(claim.getStatedDue()),
                claim.getSheetId(), claim.getSheetId(), claim.getId());
    }

    /** The person's claims, newest first. */
    public List<Claim> listOf(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY list_seq DESC, id DESC",
                ROW, sheetId, personId);
    }

    /** Save a claim's binding by its person's last statement. */
    public void updateBinding(Claim claim) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                claim.isStated(), claim.getStatedDue() == null ? null : new BigDecimal(claim.getStatedDue()),
                claim.getSheetId(), claim.getId());
    }

    /** The sheet's next claim number, taken in one atomic statement so two claims opened at the same
     *  moment never get the same one; rolled back with the request when it is refused. */
    public String nextNumber(String sheetId) {
        return String.valueOf(jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE ("
                + "UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)",
                Long.class, sheetId));
    }

    /** The next number in a period's own sequence on the sheet ({@code <period>-<n>}, from 1), or
     *  for a claim in no period ({@code period} null) in the X sequence ({@code X-<n>}); taken
     *  atomically as {@link #nextNumber} is. */
    public String nextPeriodNumber(String sheetId, String period) {
        String key = period == null ? "" : period;
        try {
            jdbc.update("INSERT INTO sheet_period_number (sheet_id, period_key) SELECT ?, ? WHERE NOT EXISTS "
                    + "(SELECT 1 FROM sheet_period_number WHERE sheet_id = ? AND period_key = ?)",
                    sheetId, key, sheetId, key);
        } catch (DuplicateKeyException raced) {
            // Another request started the sequence at the same moment.
        }
        Long n = jdbc.queryForObject("SELECT last_number FROM FINAL TABLE (UPDATE sheet_period_number "
                + "SET last_number = last_number + 1 WHERE sheet_id = ? AND period_key = ?)",
                Long.class, sheetId, key);
        return (period == null ? "X" : period) + "-" + n;
    }
}

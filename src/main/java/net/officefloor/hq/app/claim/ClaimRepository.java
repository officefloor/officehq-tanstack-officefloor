package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored claims, kept per sheet. Ids are assigned in the order claims are added. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, due, opened_by, stated, stated_due, corrects";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public ClaimRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC",
                ClaimRepository::map, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ClaimRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null, corrects);
    }

    /** The person's claim with the given number on the sheet; null when there is none. */
    public Claim findByNumber(String sheetId, long personId, String number) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                ClaimRepository::map, sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Stores the claim's values, but only while it is still a DRAFT; false when it no longer is (nothing is written). */
    public boolean update(Claim claim) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.corrects(), claim.sheetId(), claim.id(),
                ClaimStatus.DRAFT.name()) == 1;
    }

    /**
     * Stores the claim's status, number, fixed due and who opened it, but only while its status is still {@code from}; false when another
     * change got there first (nothing is written).
     */
    public boolean updateStatus(Claim claim, ClaimStatus from) {
        return jdbc.update("UPDATE claim SET status = ?, claim_number = ?, due = ?, opened_by = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.status().name(), claim.number(), claim.due(), claim.openedBy(), claim.sheetId(), claim.id(), from.name()) == 1;
    }

    /** Binds the claim to a statement figure (null: it had none), or unbinds it when {@code stated} is false. */
    public void bind(Claim claim, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                stated, stated ? statedDue : null, claim.sheetId(), claim.id());
    }

    /**
     * The sheet's next claim number: 1 for the first, then one more each time. The sheet's counter row stays locked
     * until the transaction ends, so two claims opened at once get different numbers, and a rolled back opening gives
     * its number back.
     */
    public long nextNumber(String sheetId) {
        ensureNumberRow(sheetId);
        jdbc.update("UPDATE claim_number_seq SET last_number = last_number + 1 WHERE sheet_id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_number FROM claim_number_seq WHERE sheet_id = ?", Long.class, sheetId);
    }

    /**
     * The next number in the sheet's per-period sequence {@code key} (a period's name, or X for no period): 1 for the
     * first, then one more each time, its counter row locked until the transaction ends as for {@link #nextNumber}.
     */
    public long nextPeriodNumber(String sheetId, String key) {
        ensurePeriodNumberRow(sheetId, key);
        jdbc.update("UPDATE claim_period_number_seq SET last_number = last_number + 1 WHERE sheet_id = ? AND seq_key = ?",
                sheetId, key);
        return jdbc.queryForObject("SELECT last_number FROM claim_period_number_seq WHERE sheet_id = ? AND seq_key = ?",
                Long.class, sheetId, key);
    }

    /** Adds the per-period sequence's counter row if it has none, committed at once (see ensureNumberRow). */
    private synchronized void ensurePeriodNumberRow(String sheetId, String key) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(true);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO claim_period_number_seq (sheet_id, seq_key, last_number) "
                    + "SELECT ?, ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM claim_period_number_seq WHERE sheet_id = ? AND seq_key = ?)")) {
                ps.setString(1, sheetId);
                ps.setString(2, key);
                ps.setString(3, sheetId);
                ps.setString(4, key);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Adds the sheet's counter row if it has none, on its own connection so the row is committed at once and two
     * openings at the same moment never both try to add it.
     */
    private synchronized void ensureNumberRow(String sheetId) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(true);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO claim_number_seq (sheet_id, last_number) "
                    + "SELECT ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM claim_number_seq WHERE sheet_id = ?)")) {
                ps.setString(1, sheetId);
                ps.setString(2, sheetId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Claim map(ResultSet rs, int n) throws SQLException {
        BigDecimal due = rs.getBigDecimal("due");
        BigDecimal statedDue = rs.getBigDecimal("stated_due");
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), due == null ? null : due.setScale(2),
                rs.getString("opened_by"), rs.getBoolean("stated"), statedDue == null ? null : statedDue.setScale(2),
                rs.getString("corrects"));
    }
}

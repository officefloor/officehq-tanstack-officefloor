package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The claims kept per sheet, each belonging to a person on that sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person"),
            rs.getBigDecimal("amount").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("claimed_on").toLocalDate().toString(), rs.getString("note"),
            ClaimStatus.valueOf(rs.getString("status")), rs.getObject("number_on_sheet", Integer.class),
            rs.getBigDecimal("settled_due") == null ? null
                    : rs.getBigDecimal("settled_due").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getString("opened_by"), rs.getBoolean("stated"),
            rs.getBigDecimal("stated_due") == null ? null
                    : rs.getBigDecimal("stated_due").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getString("corrects"), rs.getDate("rounds_on").toLocalDate().toString(),
            rs.getString("period_number"));

    // A claim rounds on its own date; a correction on the date of the claim it corrects (its own when none has it).
    private static final String COLUMNS = "SELECT claim.id, claim.person, claim.amount, claim.claimed_on, claim.note, claim.status,"
            + " claim.number_on_sheet, claim.settled_due, claim.opened_by, claim.stated, claim.stated_due, claim.corrects,"
            + " claim.period_number, COALESCE(corrected.claimed_on, claim.claimed_on) AS rounds_on FROM claim"
            + " LEFT JOIN claim corrected ON corrected.sheet = claim.sheet AND "
            + " COALESCE(corrected.period_number, CAST(corrected.number_on_sheet AS VARCHAR)) = claim.corrects";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every claim on the sheet, newest first. */
    public List<Claim> list(String sheet) {
        return jdbc.query(COLUMNS + " WHERE claim.sheet = ? ORDER BY claim.id DESC", ROW, sheet);
    }

    public Claim find(String sheet, long id) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ? AND claim.id = ?", ROW, sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The claim showing this number on the sheet (within its period, or on the sheet), or null when none does. */
    public Claim findByNumber(String sheet, String number) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ?"
                + " AND COALESCE(claim.period_number, CAST(claim.number_on_sheet AS VARCHAR)) = ?", ROW, sheet, number);
        return found.isEmpty() ? null : found.get(0);
    }

    public void add(String sheet, long person, ClaimInput input) {
        jdbc.update("INSERT INTO claim (sheet, person, amount, claimed_on, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                sheet, person, input.amount(), Date.valueOf(input.date()), input.note(), input.corrects());
    }

    /** Edits a draft claim: false when it is no longer a draft. */
    public boolean update(String sheet, long id, ClaimInput input) {
        return jdbc.update(
                "UPDATE claim SET amount = ?, claimed_on = ?, note = ?, corrects = ? WHERE sheet = ? AND id = ? AND status = 'DRAFT'",
                input.amount(), Date.valueOf(input.date()), input.note(), input.corrects(), sheet, id) == 1;
    }

    /** Moves the claim from one status to another: false when it no longer has the status it was read with. */
    public boolean changeStatus(String sheet, long id, ClaimStatus from, ClaimStatus to) {
        return jdbc.update("UPDATE claim SET status = ? WHERE sheet = ? AND id = ? AND status = ?", to.name(), sheet,
                id, from.name()) == 1;
    }

    /** Fixes the claim's due as it is settled, or leaves none with null. */
    public void setSettledDue(String sheet, long id, BigDecimal due) {
        jdbc.update("UPDATE claim SET settled_due = ? WHERE sheet = ? AND id = ?", due, sheet, id);
    }

    /** Binds the claim to a statement's figure (null when its grade had no rate), or unbinds it when not stated. */
    public void setStated(String sheet, long id, boolean stated, BigDecimal due) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet = ? AND id = ?", stated, due, sheet, id);
    }

    /** Records the user who opened the claim. */
    public void setOpenedBy(String sheet, long id, String user) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE sheet = ? AND id = ?", user, sheet, id);
    }

    /** Sets the claim's number on the sheet, or clears it with null. */
    public void setNumber(String sheet, long id, Integer number) {
        jdbc.update("UPDATE claim SET number_on_sheet = ? WHERE sheet = ? AND id = ?", number, sheet, id);
    }

    /** Sets the claim's number within its period. */
    public void setPeriodNumber(String sheet, long id, String periodNumber) {
        jdbc.update("UPDATE claim SET period_number = ? WHERE sheet = ? AND id = ?", periodNumber, sheet, id);
    }

    /**
     * Takes the next number in the sheet's period of the name ("" for the claims in no period): 1 first, then 2,
     * and so on, each period counting on its own. Locked as {@link #nextNumber}.
     */
    public int nextPeriodNumber(String sheet, String period) {
        while (true) {
            if (jdbc.update("UPDATE claim_period_number SET last_number = last_number + 1 WHERE sheet = ? AND period = ?",
                    sheet, period) == 1) {
                return jdbc.queryForObject("SELECT last_number FROM claim_period_number WHERE sheet = ? AND period = ?",
                        Integer.class, sheet, period);
            }
            try {
                jdbc.update("INSERT INTO claim_period_number (sheet, period, last_number) VALUES (?, ?, 1)", sheet, period);
                return 1;
            } catch (DuplicateKeyException raced) {
                // Another user took the period's first number at the same moment: take the one after it.
            }
        }
    }

    /**
     * Takes the sheet's next claim number: 1 first, then 2, and so on. The counter row stays locked until the
     * transaction ends, so two claims opened at once get different numbers.
     */
    public int nextNumber(String sheet) {
        while (true) {
            if (jdbc.update("UPDATE claim_number SET last_number = last_number + 1 WHERE sheet = ?", sheet) == 1) {
                return jdbc.queryForObject("SELECT last_number FROM claim_number WHERE sheet = ?", Integer.class,
                        sheet);
            }
            try {
                jdbc.update("INSERT INTO claim_number (sheet, last_number) VALUES (?, 1)", sheet);
                return 1;
            } catch (DuplicateKeyException raced) {
                // Another user took the sheet's first number at the same moment: take the one after it.
            }
        }
    }
}

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
            rs.getString("opened_by"));

    private static final String COLUMNS = "SELECT id, person, amount, claimed_on, note, status, number_on_sheet, settled_due, opened_by FROM claim";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every claim on the sheet, newest first. */
    public List<Claim> list(String sheet) {
        return jdbc.query(COLUMNS + " WHERE sheet = ? ORDER BY id DESC", ROW, sheet);
    }

    public Claim find(String sheet, long id) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE sheet = ? AND id = ?", ROW, sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public void add(String sheet, long person, ClaimInput input) {
        jdbc.update("INSERT INTO claim (sheet, person, amount, claimed_on, note) VALUES (?, ?, ?, ?, ?)", sheet, person,
                input.amount(), Date.valueOf(input.date()), input.note());
    }

    public void update(String sheet, long id, ClaimInput input) {
        jdbc.update("UPDATE claim SET amount = ?, claimed_on = ?, note = ? WHERE sheet = ? AND id = ?", input.amount(),
                Date.valueOf(input.date()), input.note(), sheet, id);
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

    /** Records the user who opened the claim. */
    public void setOpenedBy(String sheet, long id, String user) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE sheet = ? AND id = ?", user, sheet, id);
    }

    /** Sets the claim's number on the sheet, or clears it with null. */
    public void setNumber(String sheet, long id, Integer number) {
        jdbc.update("UPDATE claim SET number_on_sheet = ? WHERE sheet = ? AND id = ?", number, sheet, id);
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

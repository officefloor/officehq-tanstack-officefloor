package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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
            rs.getString("period_number"), rs.getDate("due_by").toLocalDate().toString(), List.of(),
            rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate().toString(),
            rs.getBoolean("deferred"), rs.getInt("proofs"), rs.getString("split_from"),
            rs.getString("locked_by"));

    // A claim rounds on its own date; a correction on the date of the claim it corrects (its own when none has it).
    // It is deferred while OPEN with its deferred-until after the sheet's today (the real date until one is set).
    private static final String COLUMNS = "SELECT claim.id, claim.person, claim.amount, claim.claimed_on, claim.note, claim.status,"
            + " claim.number_on_sheet, claim.settled_due, claim.opened_by, claim.stated, claim.stated_due, claim.corrects,"
            + " claim.period_number, claim.due_by, COALESCE(corrected.claimed_on, claim.claimed_on) AS rounds_on,"
            + " claim.deferred_until, claim.proofs, claim.split_from, claim.locked_by, (claim.status = 'OPEN' AND claim.deferred_until IS NOT NULL AND claim.deferred_until >"
            + " COALESCE((SELECT s.today FROM sheet s WHERE s.id = claim.sheet), CURRENT_DATE)) AS deferred FROM claim"
            + " LEFT JOIN claim corrected ON corrected.sheet = claim.sheet AND "
            + " COALESCE(corrected.period_number, CAST(corrected.number_on_sheet AS VARCHAR)) = claim.corrects";

    // The place after every claim listed on the sheet (bound to the sheet), so the claim lists newest.
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(l.listed), 0) + 1 FROM claim l WHERE l.sheet = ?)";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String LOG = "SELECT claim, status, by_user, on_date, due FROM claim_log WHERE sheet = ?";

    /** Every claim on the sheet, newest first. */
    public List<Claim> list(String sheet) {
        return withLogs(sheet, jdbc.query(COLUMNS + " WHERE claim.sheet = ? ORDER BY claim.listed DESC, claim.id DESC", ROW, sheet),
                jdbc.query(LOG + " ORDER BY id", LOG_ROW, sheet));
    }

    public Claim find(String sheet, long id) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ? AND claim.id = ?", ROW, sheet, id);
        return found.isEmpty() ? null
                : withLogs(sheet, found, jdbc.query(LOG + " AND claim = ? ORDER BY id", LOG_ROW, sheet, id)).get(0);
    }

    private record Logged(long claim, ClaimLogLine line) {
    }

    private static final RowMapper<Logged> LOG_ROW = (rs, i) -> new Logged(rs.getLong("claim"),
            new ClaimLogLine(ClaimStatus.valueOf(rs.getString("status")), rs.getString("by_user"),
                    rs.getDate("on_date").toLocalDate().toString(), rs.getString("due")));

    /** The claims, each with its own lines of the status log (oldest first). */
    private static List<Claim> withLogs(String sheet, List<Claim> found, List<Logged> logged) {
        Map<Long, List<ClaimLogLine>> lines = new HashMap<>();
        for (Logged l : logged) {
            lines.computeIfAbsent(l.claim(), k -> new ArrayList<>()).add(l.line());
        }
        return found.stream().map(c -> c.withLog(lines.getOrDefault(c.id(), List.of()))).toList();
    }

    /** The claim showing this number on the sheet (within its period, or on the sheet), or null when none does. */
    public Claim findByNumber(String sheet, String number) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ?"
                + " AND COALESCE(claim.period_number, CAST(claim.number_on_sheet AS VARCHAR)) = ?", ROW, sheet, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Adds the claim with its due-by, fixed now and never moved after; returns its id. */
    public long add(String sheet, long person, ClaimInput input, LocalDate dueBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO claim (sheet, person, amount, claimed_on, note, corrects, due_by, listed)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, " + NEXT_LISTED + ")", new String[] { "id" });
            ps.setString(1, sheet);
            ps.setLong(2, person);
            ps.setBigDecimal(3, input.amount());
            ps.setDate(4, Date.valueOf(input.date()));
            ps.setString(5, input.note());
            ps.setString(6, input.corrects());
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setString(8, sheet);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /**
     * Adds the OPEN claim split off another: the amount, with the date and note of the claim it was split from (by
     * the number that claim shows), opened by the user, with its due-by fixed now; returns its id.
     */
    public long addSplit(String sheet, long person, BigDecimal amount, LocalDate date, String note, LocalDate dueBy,
            String splitFrom, String openedBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO claim (sheet, person, amount, claimed_on, note, due_by, status,"
                    + " split_from, opened_by, listed) VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, " + NEXT_LISTED + ")",
                    new String[] { "id" });
            ps.setString(1, sheet);
            ps.setLong(2, person);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setDate(6, Date.valueOf(dueBy));
            ps.setString(7, splitFrom);
            ps.setString(8, openedBy);
            ps.setString(9, sheet);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /**
     * Leaves an OPEN claim the rest of its amount once split, unbound from any statement; conditional on the
     * amount read and on it still being OPEN and not deferred: false when it no longer is.
     */
    public boolean split(String sheet, long id, BigDecimal was, BigDecimal rest) {
        return jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE sheet = ? AND id = ?"
                + " AND status = 'OPEN' AND amount = ? AND (deferred_until IS NULL OR deferred_until <="
                + " COALESCE((SELECT s.today FROM sheet s WHERE s.id = claim.sheet), CURRENT_DATE))",
                rest, sheet, id, was) == 1;
    }

    /**
     * Edits a draft claim not locked by another user's editor, releasing any lock on it: false when it is no longer a
     * draft, or another user's editor holds it.
     */
    public boolean update(String sheet, long id, ClaimInput input, String user) {
        return jdbc.update(
                "UPDATE claim SET amount = ?, claimed_on = ?, note = ?, corrects = ?, locked_by = NULL WHERE sheet = ? AND id = ?"
                        + " AND status = 'DRAFT' AND (locked_by IS NULL OR locked_by = ?)",
                input.amount(), Date.valueOf(input.date()), input.note(), input.corrects(), sheet, id, user) == 1;
    }

    /**
     * Locks a draft claim for the user's editor, releasing any other claim the user's editor held on the sheet:
     * false when it is no longer a draft, or another user's editor holds it.
     */
    public boolean lock(String sheet, long id, String user) {
        if (jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet = ? AND id = ? AND status = 'DRAFT'"
                + " AND (locked_by IS NULL OR locked_by = ?)", user, sheet, id, user) != 1) {
            return false;
        }
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND id <> ? AND locked_by = ?", sheet, id, user);
        return true;
    }

    /** Releases the claim's lock when the user's editor holds it. */
    public void unlock(String sheet, long id, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND id = ? AND locked_by = ?", sheet, id, user);
    }

    /** Releases every lock on the sheet's claims, as when its today changes. */
    public void unlockAll(String sheet) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND locked_by IS NOT NULL", sheet);
    }

    /**
     * Moves the claim of the person and status it was read with to another person, unbound from any statement and
     * listed as the sheet's newest: false when it no longer has that person or status.
     */
    public boolean reassign(String sheet, long id, long from, ClaimStatus status, long to) {
        return jdbc.update("UPDATE claim SET person = ?, stated = FALSE, stated_due = NULL, listed = " + NEXT_LISTED
                + " WHERE sheet = ? AND id = ? AND person = ? AND status = ?", to, sheet, sheet, id, from,
                status.name()) == 1;
    }

    /**
     * Moves the claim of the status and date it was read with to another date, keeping its due-by and any figure its
     * statement bound: false when it no longer has that status or date.
     */
    public boolean redate(String sheet, long id, ClaimStatus status, LocalDate was, LocalDate date) {
        return jdbc.update("UPDATE claim SET claimed_on = ? WHERE sheet = ? AND id = ? AND status = ? AND claimed_on = ?",
                Date.valueOf(date), sheet, id, status.name(), Date.valueOf(was)) == 1;
    }

    /** Moves the claim from one status to another: false when it no longer has the status it was read with. */
    public boolean changeStatus(String sheet, long id, ClaimStatus from, ClaimStatus to) {
        return jdbc.update("UPDATE claim SET status = ? WHERE sheet = ? AND id = ? AND status = ?", to.name(), sheet,
                id, from.name()) == 1;
    }

    /** Defers an OPEN claim until the date: false when it is no longer OPEN. */
    public boolean defer(String sheet, long id, LocalDate until) {
        return jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet = ? AND id = ? AND status = 'OPEN'",
                Date.valueOf(until), sheet, id) == 1;
    }

    /**
     * Raises the claim's proofs count by one, conditional on the status and count read, so a concurrent change by
     * another user is refused, not overwritten: false when it was not.
     */
    public boolean addProof(String sheet, long id, ClaimStatus status, int was) {
        return jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE sheet = ? AND id = ? AND status = ? AND proofs = ?",
                sheet, id, status.name(), was) == 1;
    }

    /** Fixes the claim's due as it is settled, or leaves none with null. */
    public void setSettledDue(String sheet, long id, BigDecimal due) {
        jdbc.update("UPDATE claim SET settled_due = ? WHERE sheet = ? AND id = ?", due, sheet, id);
    }

    /**
     * Re-fixes a SETTLED claim's due (null for none), conditional on the due read, so a concurrent change by another
     * user is refused, not overwritten; false when it was not.
     */
    public boolean resettle(String sheet, long id, BigDecimal was, BigDecimal due) {
        return jdbc.update("UPDATE claim SET settled_due = ? WHERE sheet = ? AND id = ? AND status = ?"
                + " AND settled_due IS NOT DISTINCT FROM ?", due, sheet, id, ClaimStatus.SETTLED.name(), was) == 1;
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

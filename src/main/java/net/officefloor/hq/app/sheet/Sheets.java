package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheet store. {@link #today(String)} is THE date for a sheet: every feature that decides
 * anything by date, or fills in a default date, asks it — never {@code LocalDate.now()}.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today; a sheet seen for the first time starts at the real date, kept from then on. */
    public LocalDate today(String sheetId) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        LocalDate now = LocalDate.now();
        try {
            jdbc.update("INSERT INTO sheet (id, today) VALUES (?, ?)", sheetId, Date.valueOf(now));
        } catch (DuplicateKeyException raced) {
            // another request started this sheet first; its date stands
        }
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
    }

    /**
     * The sheet's next claim number: the sheets of a tree share one sequence, so it is the next after
     * the last number given on any sheet of the sheet's tree. The tree's rows stay locked until the
     * transaction ends, so claims opened at the same moment get distinct numbers, and a rolled-back
     * open gives none away.
     */
    public long nextClaimNumber(String sheetId) {
        today(sheetId); // the sheet's row holds the counter
        List<String> tree = new ArrayList<>(tree(sheetId));
        tree.sort(null); // lock in one order so concurrent opens across the tree cannot deadlock
        long last = 0;
        for (String id : tree) {
            List<Long> found = jdbc.query("SELECT last_claim_number FROM sheet WHERE id = ? FOR UPDATE",
                    (rs, i) -> rs.getLong(1), id);
            if (!found.isEmpty()) {
                last = Math.max(last, found.get(0));
            }
        }
        jdbc.update("UPDATE sheet SET last_claim_number = ? WHERE id = ?", last + 1, sheetId);
        return last + 1;
    }

    /** The sheet's next number in the fiscal year's sequence, shared across its tree like {@link #nextClaimNumber(String)}. */
    public long nextFiscalClaimNumber(String sheetId, String fiscalYear) {
        today(sheetId);
        List<String> tree = new ArrayList<>(tree(sheetId));
        tree.sort(null); // lock in one order so concurrent opens across the tree cannot deadlock
        long last = 0;
        for (String id : tree) {
            jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), id);
            List<Long> found = jdbc.query("SELECT last_number FROM fiscal_claim_number WHERE sheet_id = ? AND fiscal_year = ?",
                    (rs, i) -> rs.getLong(1), id, fiscalYear);
            if (!found.isEmpty()) {
                last = Math.max(last, found.get(0));
            }
        }
        jdbc.update("MERGE INTO fiscal_claim_number (sheet_id, fiscal_year, last_number) KEY (sheet_id, fiscal_year) VALUES (?, ?, ?)",
                sheetId, fiscalYear, last + 1);
        return last + 1;
    }

    /** The sheets of the sheet's tree: its root (the top of its parent chain), and every descendant of the root. */
    public Set<String> tree(String sheetId) {
        String root = sheetId;
        Set<String> up = new HashSet<>(List.of(sheetId));
        for (String parent = parent(root); parent != null && up.add(parent); parent = parent(root)) {
            root = parent;
        }
        Set<String> tree = new HashSet<>(List.of(root, sheetId));
        tree.addAll(descendants(root));
        return tree;
    }

    /** The sheet's next bulk settle batch number, counted like {@link #nextClaimNumber(String)}. */
    public int nextSettleBatch(String sheetId) {
        today(sheetId); // the sheet's row holds the counter
        jdbc.update("UPDATE sheet SET last_settle_batch = last_settle_batch + 1 WHERE id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    /** The number of the sheet's last bulk settle batch; 0 when there has been none. */
    public int lastSettleBatch(String sheetId) {
        today(sheetId);
        return jdbc.queryForObject("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    /** Whether the sheet exists: it has been loaded. */
    public boolean exists(String sheetId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId) > 0;
    }

    /** The sheet's parent sheet id; null when none. */
    public String parent(String sheetId) {
        List<String> found = jdbc.query("SELECT parent FROM sheet WHERE id = ?", (rs, i) -> rs.getString(1), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet the sheet was cloned from; null when it was not cloned. */
    public String source(String sheetId) {
        List<String> found = jdbc.query("SELECT source FROM sheet WHERE id = ?", (rs, i) -> rs.getString(1), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's descendants: the sheets naming it as their parent, their children and so on, nearest first. */
    public List<String> descendants(String sheetId) {
        List<String> found = new ArrayList<>();
        Set<String> seen = new HashSet<>(List.of(sheetId));
        for (int at = -1; at < found.size(); at++) {
            String of = at < 0 ? sheetId : found.get(at);
            for (String child : jdbc.query("SELECT id FROM sheet WHERE parent = ? ORDER BY id", (rs, i) -> rs.getString(1), of)) {
                if (seen.add(child)) {
                    found.add(child);
                }
            }
        }
        return found;
    }

    public void setParent(String sheetId, String parent) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET parent = ? WHERE id = ?", parent, sheetId);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }

    /** Whether the sheet is archived: everything on it stays visible and every change to it is refused. */
    public boolean archived(String sheetId) {
        today(sheetId); // the sheet's row holds the flag
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT archived FROM sheet WHERE id = ?", Boolean.class, sheetId));
    }

    public void archive(String sheetId) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET archived = TRUE, archived_on = today WHERE id = ?", sheetId);
    }
}

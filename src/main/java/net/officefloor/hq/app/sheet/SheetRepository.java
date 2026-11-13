package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, created empty (today = the real date) when it was never loaded before. */
    public Sheet ensure(String id) {
        jdbc.update("INSERT INTO sheet (id, today, started) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                id, Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now()), id);
        return find(id);
    }

    /** The sheet with this id, or null when it was never loaded. */
    public Sheet find(String id) {
        List<Sheet> found = jdbc.query("SELECT id, today, archived FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getDate("today").toLocalDate(), rs.getBoolean("archived")),
                id);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The sheet's today. Use this — never {@code LocalDate.now()} — for any date-dependent decision
     * or default date in the sheet.
     */
    public LocalDate today(String id) {
        Sheet sheet = find(id);
        return sheet == null ? LocalDate.now() : sheet.today();
    }

    /**
     * The date a new claim left without one takes: the sheet's entry date while it is set, else its today.
     */
    public LocalDate entryDate(String id) {
        List<Date> found = jdbc.query("SELECT entry_date FROM sheet WHERE id = ?", (rs, n) -> rs.getDate("entry_date"), id);
        return found.isEmpty() || found.get(0) == null ? today(id) : found.get(0).toLocalDate();
    }

    /**
     * Takes the next claim number of the sheet's tree (1 for its first): one after the last number given on any sheet
     * of the tree, in one atomic statement that moves every sheet of the tree on to it, so two claims opened at the
     * same moment never get the same number; a number taken is never given out again.
     */
    public int nextClaimNumber(String id) {
        List<String> tree = tree(id);
        String in = String.join(", ", Collections.nCopies(tree.size(), "?"));
        Object[] args = new Object[tree.size() * 2];
        for (int i = 0; i < tree.size(); i++) {
            args[i] = tree.get(i);
            args[tree.size() + i] = tree.get(i);
        }
        List<Integer> taken = jdbc.queryForList("SELECT last_claim_number FROM FINAL TABLE (UPDATE sheet SET"
                + " last_claim_number = (SELECT MAX(t.last_claim_number) FROM sheet t WHERE t.id IN (" + in + ")) + 1"
                + " WHERE id IN (" + in + "))", Integer.class, args);
        return taken.get(0);
    }

    /**
     * The ids of the sheets of the sheet's tree, the sheet among them: its topmost existing ancestor and every sheet
     * below that (the sheets naming it as their parent, their children and so on).
     */
    public List<String> tree(String id) {
        Set<String> seen = new HashSet<>(List.of(id));
        String root = id;
        for (String up = parent(id); up != null && find(up) != null && seen.add(up); up = parent(up)) {
            root = up;
        }
        LinkedHashSet<String> found = new LinkedHashSet<>(List.of(root, id));
        List<String> todo = new ArrayList<>(List.of(root));
        while (!todo.isEmpty()) {
            String at = todo.remove(0);
            for (String child : jdbc.queryForList("SELECT id FROM sheet WHERE parent_id = ? ORDER BY id", String.class,
                    at)) {
                if (found.add(child)) {
                    todo.add(child);
                }
            }
        }
        return List.copyOf(found);
    }

    /**
     * Takes the next number (1 for its first) of one of the sheet's per-period sequences: {@code key} is the
     * period's name in lower case, or "" for the claims in no period. A number taken is never given out again.
     */
    public int nextPeriodClaimNumber(String id, String key) {
        for (int attempt = 0; ; attempt++) {
            List<Integer> taken = jdbc.queryForList("SELECT last_number FROM FINAL TABLE (UPDATE period_claim_number"
                    + " SET last_number = last_number + 1 WHERE sheet_id = ? AND seq_key = ?)", Integer.class, id, key);
            if (!taken.isEmpty()) {
                return taken.get(0);
            }
            try {
                jdbc.update("INSERT INTO period_claim_number (sheet_id, seq_key, last_number) VALUES (?, ?, 1)", id, key);
                return 1;
            } catch (DuplicateKeyException e) {
                if (attempt > 0) {
                    throw e;
                }
                // another request started the sequence at the same moment: take the next from it
            }
        }
    }

    /** The sheet's parent sheet id as the store holds it now (null: none); it is not dated, so an as-of view shows it too. */
    public String parent(String id) {
        List<String> found = jdbc.query("SELECT parent_id FROM PUBLIC.sheet WHERE id = ?", (rs, n) -> rs.getString("parent_id"), id);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The sheet's descendants: its children (the sheets naming it as their parent), their children and so on,
     * each once, nearest first; the sheet itself never, even where parents loop back to it.
     */
    public List<String> descendants(String id) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>(List.of(id));
        while (!todo.isEmpty()) {
            String at = todo.poll();
            for (String child : jdbc.queryForList("SELECT id FROM PUBLIC.sheet WHERE parent_id = ? ORDER BY id", String.class, at)) {
                if (!child.equals(id) && found.add(child)) {
                    todo.add(child);
                }
            }
        }
        return List.copyOf(found);
    }

    /** Whether the sheet is archived: every change to an archived sheet is refused. */
    public boolean isArchived(String id) {
        Sheet sheet = find(id);
        return sheet != null && sheet.archived();
    }

    public void save(Sheet sheet) {
        jdbc.update("UPDATE sheet SET today = ?, archived = ? WHERE id = ?", Date.valueOf(sheet.today()),
                sheet.archived(), sheet.id());
    }
}

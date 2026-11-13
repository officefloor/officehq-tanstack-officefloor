package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The names and aliases used across a sheet's tree: its ancestors (its parent, their parent and so on) and its
 * descendants (the sheets naming it as their parent, their children and so on), among the sheets that exist, never the
 * sheet itself. A name or alias one of them uses, ignoring case, may not be used on the sheet; it is read from the store
 * as it stands now, whatever date the sheet is viewed as of.
 */
@Component
public class NameTree {

    private final JdbcTemplate jdbc;

    public NameTree(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of the other sheets in the sheet's tree, ancestors nearest first, then descendants nearest first. */
    public List<String> sheets(String sheetId) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>(List.of(sheetId));
        for (String up = parent(sheetId); up != null && seen.add(up); up = parent(up)) {
            if (!exists(up)) {
                break;
            }
            found.add(up);
        }
        List<String> todo = new ArrayList<>(List.of(sheetId));
        Set<String> below = new HashSet<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            String at = todo.remove(0);
            for (String child : jdbc.queryForList("SELECT id FROM PUBLIC.sheet WHERE parent_id = ? ORDER BY id",
                    String.class, at)) {
                if (below.add(child)) {
                    found.add(child);
                    todo.add(child);
                }
            }
        }
        return List.copyOf(found);
    }

    /** Every name and alias the other sheets of the sheet's tree use, lower-cased. */
    public Set<String> taken(String sheetId) {
        return taken(sheetId, null);
    }

    /**
     * Every name and alias the other sheets of the sheet's tree use, lower-cased, leaving out those of the person
     * {@code exceptPersonId} (null: none), whose names leave their sheet with them.
     */
    public Set<String> taken(String sheetId, Long exceptPersonId) {
        Set<String> keys = new HashSet<>();
        for (String other : sheets(sheetId)) {
            keys.addAll(jdbc.queryForList("SELECT name_key FROM PUBLIC.person WHERE sheet_id = ? AND (? IS NULL OR id <> ?)",
                    String.class, other, exceptPersonId, exceptPersonId));
            keys.addAll(jdbc.queryForList("SELECT alias_key FROM PUBLIC.person_alias WHERE sheet_id = ?"
                    + " AND (? IS NULL OR person_id <> ?)", String.class, other, exceptPersonId, exceptPersonId));
        }
        return keys;
    }

    /** Whether another sheet of the sheet's tree uses the name as a name or alias, ignoring case. */
    public boolean clashes(String sheetId, String name) {
        return taken(sheetId).contains(key(name));
    }

    /** Whether the person's name or any of their aliases is used by another sheet of their sheet's tree. */
    public boolean clashing(Person person, List<String> aliases) {
        Set<String> taken = taken(person.sheetId());
        return clashing(taken, person.name(), aliases);
    }

    /** Whether the name or any of the aliases is among the taken keys. */
    public static boolean clashing(Set<String> taken, String name, List<String> aliases) {
        if (taken.isEmpty()) {
            return false;
        }
        if (taken.contains(key(name))) {
            return true;
        }
        for (String alias : aliases) {
            if (taken.contains(key(alias))) {
                return true;
            }
        }
        return false;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String parent(String id) {
        List<String> found = jdbc.query("SELECT parent_id FROM PUBLIC.sheet WHERE id = ?",
                (rs, n) -> rs.getString("parent_id"), id);
        return found.isEmpty() ? null : found.get(0);
    }

    private boolean exists(String id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PUBLIC.sheet WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }
}

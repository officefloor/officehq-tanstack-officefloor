package net.officefloor.hq.app.person;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * The names and aliases in use on the other sheets of a sheet's tree: its ancestors (its parent, its parent's parent,
 * and so on) and its descendants (its children, their children, and so on), among the sheets that exist. A name or
 * alias may not be used on a sheet when any of them already uses it, ignoring case.
 */
@Service
public class TreeNames {
    private final SheetRepository sheets;
    private final PersonRepository people;
    private final PersonAliasRepository aliases;

    public TreeNames(SheetRepository sheets, PersonRepository people, PersonAliasRepository aliases) {
        this.sheets = sheets;
        this.people = people;
        this.aliases = aliases;
    }

    /** The ids of the other sheets of the sheet's tree. */
    public Set<String> treeOf(String sheetId) {
        Set<String> tree = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        seen.add(sheetId);
        for (String id = parentOf(sheetId); id != null && seen.add(id); id = parentOf(id)) {
            if (sheets.existsById(id)) {
                tree.add(id);
            }
        }
        Deque<String> todo = new ArrayDeque<>(List.of(sheetId));
        Set<String> visited = new HashSet<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : sheets.childrenOf(todo.pop())) {
                if (visited.add(child)) {
                    tree.add(child);
                    todo.add(child);
                }
            }
        }
        tree.remove(sheetId);
        return tree;
    }

    /** The names and aliases, lower-cased, in use on the other sheets of the sheet's tree. */
    public Set<String> usedAround(String sheetId) {
        Set<String> tree = treeOf(sheetId);
        Set<String> used = new HashSet<>();
        if (tree.isEmpty()) {
            return used;
        }
        people.findBySheetIdIn(tree).forEach(p -> used.add(key(p.getName())));
        aliases.findBySheetIdIn(tree).forEach(a -> used.add(key(a.getAlias())));
        return used;
    }

    /** Whether any of the names is in use, ignoring case, on another sheet of the sheet's tree. */
    public boolean usedAround(String sheetId, Collection<String> names) {
        Set<String> used = usedAround(sheetId);
        return names.stream().anyMatch(n -> used.contains(key(n)));
    }

    /** Whether the name is in use, ignoring case, on another sheet of the sheet's tree. */
    public boolean usedAround(String sheetId, String name) {
        return usedAround(sheetId, List.of(name));
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String parentOf(String sheetId) {
        return sheets.parentsOf(sheetId).stream().filter(p -> p != null).findFirst().orElse(null);
    }
}

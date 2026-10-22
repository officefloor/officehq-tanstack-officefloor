package net.officefloor.hq.app.setting;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.role.Role;

/** A sheet's permission matrix: for each action the roles that may take it, as a comma separated list ('' when
 *  blank: the standing rule applies — open, settle, void and close by clerk and steward; reassign and reopen by
 *  steward). */
public record Permissions(Map<String, String> roles) {

    /** The actions the matrix covers, in order. */
    public static final List<String> ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** The standing rule for each action, used while its entry is blank. */
    private static final Map<String, List<String>> STANDING = Map.of(
            "open", List.of(Role.CLERK, Role.STEWARD), "settle", List.of(Role.CLERK, Role.STEWARD),
            "void", List.of(Role.CLERK, Role.STEWARD), "close", List.of(Role.CLERK, Role.STEWARD),
            "reassign", List.of(Role.STEWARD), "reopen", List.of(Role.STEWARD));

    /** The entry for each action, in order ('' when blank). */
    public Map<String, String> entries() {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String action : ACTIONS) {
            entries.put(action, roles.getOrDefault(action, ""));
        }
        return entries;
    }

    /** Whether the role may take the action. */
    public boolean allows(String action, String role) {
        String entry = roles.getOrDefault(action, "");
        List<String> allowed = entry.isBlank() ? STANDING.getOrDefault(action, List.of())
                : Arrays.stream(entry.split(",")).map(String::trim).toList();
        return allowed.contains(role);
    }
}

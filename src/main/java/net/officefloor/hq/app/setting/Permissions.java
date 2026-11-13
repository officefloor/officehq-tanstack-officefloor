package net.officefloor.hq.app.setting;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A sheet's permission matrix: for each action set, the roles allowed it, as typed and shown (comma separated role
 * labels). An action not set keeps the standing rule: open, settle, void and close by clerk and steward; reassign and
 * reopen by steward. Not dated.
 */
public record Permissions(Map<String, String> set) {

    public static final String OPEN = "open";
    public static final String SETTLE = "settle";
    public static final String VOID = "void";
    public static final String REASSIGN = "reassign";
    public static final String CLOSE = "close";
    public static final String REOPEN = "reopen";

    /** The actions the matrix covers, in the order they are shown and recorded. */
    public static final List<String> ACTIONS = List.of(OPEN, SETTLE, VOID, REASSIGN, CLOSE, REOPEN);

    /** The setting key of an action's entry: perm-&lt;action&gt;. */
    public static String key(String action) {
        return "perm-" + action;
    }

    /** The roles allowed the action when none are set. */
    private static Set<Role> standing(String action) {
        return switch (action) {
            case REASSIGN, REOPEN -> EnumSet.of(Role.STEWARD);
            default -> EnumSet.of(Role.CLERK, Role.STEWARD);
        };
    }

    /** The roles in a comma separated list as typed (each case and outer spaces ignored), or null when one is not a role. */
    public static Set<Role> parse(String roles) {
        Set<Role> parsed = EnumSet.noneOf(Role.class);
        for (String label : roles.split(",", -1)) {
            Role role = Role.of(label);
            if (role == null) {
                return null;
            }
            parsed.add(role);
        }
        return parsed;
    }

    /** The roles as shown: their labels, comma separated, in the order typed without repeats. */
    public static String label(String roles) {
        return Arrays.stream(roles.split(",", -1)).map(Role::of).map(Role::label).distinct()
                .collect(Collectors.joining(","));
    }

    /** Whether the role may take the action. */
    public boolean allows(String action, Role role) {
        String roles = set.get(action);
        return (roles == null ? standing(action) : parse(roles)).contains(role);
    }

    /** Each action's entry as shown, by action ("": blank, the standing rule). */
    public Map<String, String> entries() {
        Map<String, String> entries = new LinkedHashMap<>();
        ACTIONS.forEach((a) -> entries.put(a, set.getOrDefault(a, "")));
        return entries;
    }
}

package net.officefloor.hq.app.setting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The permission matrix: for each guarded action, the roles that may take it ({@code perm-<action>},
 *  a comma separated list of roles; blank: the standing rule). Approving and rejecting are not in it:
 *  they stay with stewards. */
public final class Permissions {
    public static final String OPEN = "open";
    public static final String SETTLE = "settle";
    public static final String VOID = "void";
    public static final String REASSIGN = "reassign";
    public static final String CLOSE = "close";
    public static final String REOPEN = "reopen";
    public static final String TODAY = "today";

    /** The key prefix of an action's entry. */
    public static final String PREFIX = "perm-";

    /** The standing rule: the roles that may take each action while its entry is blank. */
    public static final Map<String, List<String>> STANDING = Map.of(
            OPEN, List.of(Role.CLERK, Role.STEWARD),
            SETTLE, List.of(Role.CLERK, Role.STEWARD),
            VOID, List.of(Role.CLERK, Role.STEWARD),
            CLOSE, List.of(Role.CLERK, Role.STEWARD),
            REASSIGN, List.of(Role.STEWARD),
            REOPEN, List.of(Role.STEWARD),
            TODAY, List.of(Role.CLERK, Role.STEWARD, Role.RESTRICTED));

    private Permissions() {
    }

    /** Whether the key is an action's entry in the matrix. */
    public static boolean isKey(String key) {
        return key.startsWith(PREFIX) && STANDING.containsKey(key.substring(PREFIX.length()));
    }

    /** The roles a stored entry names (blank: none). */
    public static List<String> roles(String value) {
        return value == null || value.isBlank() ? List.of()
                : Arrays.stream(value.split(",")).map(String::trim).toList();
    }

    /** Whether the user's role on the sheet may take the action. */
    public static boolean allows(String sheetId, String user, String action, RoleRepository roles,
            SettingRepository settings) {
        List<String> allowed = roles(settings.find(sheetId, PREFIX + action));
        if (allowed.isEmpty()) allowed = STANDING.get(action);
        return allowed.contains(roles.roleOf(sheetId, user == null ? "" : user));
    }
}

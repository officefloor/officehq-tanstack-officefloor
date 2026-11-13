package net.officefloor.hq.app.setting;

import java.util.Map;

/** The permission matrix entries a save carries, by action, in matrix order: the roles as shown ("": blank, the standing rule). */
public record PermissionChanges(Map<String, String> entries) {
}

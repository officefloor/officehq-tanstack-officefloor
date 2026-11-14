package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;

/** A role as the form submits it: the user name and the role (clerk, steward, viewer or restricted). */
public record RoleRequest(@Size(max = 100) String user, @Size(max = 30) String role) {
}

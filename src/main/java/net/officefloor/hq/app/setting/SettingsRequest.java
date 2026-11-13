package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;

/** The settings as the form submits them: the clerk limit (two decimals at most; blank: no limit). */
public record SettingsRequest(@Size(max = 30) String clerkLimit) {
}

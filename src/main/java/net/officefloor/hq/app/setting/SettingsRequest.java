package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;

/**
 * The settings as the form submits them; a setting left out (null) is left as it is. The clerk limit has two
 * decimals at most (blank: no limit); the floor has two decimals at most (blank: back to 1.00); the terms are
 * whole days, 0 or more (blank: back to 14); the late percent has two decimals at most (blank: back to 0.00).
 */
public record SettingsRequest(@Size(max = 30) String clerkLimit, @Size(max = 30) String floor, @Size(max = 30) String terms,
        @Size(max = 30) String latePct) {
}

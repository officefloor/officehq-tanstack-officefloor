package net.officefloor.hq.app.settings;

import java.util.List;

/** A sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be) and the roles set on it, by user name. A name not listed is a clerk. */
public record Settings(String clerkLimit, String floor, List<Role> roles) {
}

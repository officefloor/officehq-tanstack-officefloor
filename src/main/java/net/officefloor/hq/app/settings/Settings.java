package net.officefloor.hq.app.settings;

import java.util.List;

/** A sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be), its terms (the days after a claim's date that it is due by) and the roles set on it, by user name. A name not listed is a clerk. */
public record Settings(String clerkLimit, String floor, int terms, List<Role> roles) {
}

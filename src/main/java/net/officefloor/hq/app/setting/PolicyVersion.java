package net.officefloor.hq.app.setting;

/** A version of the sheet's policy: its number (from 1, in the order the changes were made), the policy
 *  setting whose change made it, and the from-date it is in force from (yyyy-MM-dd; null: always). */
public record PolicyVersion(String sheetId, int version, String key, String from) {
}

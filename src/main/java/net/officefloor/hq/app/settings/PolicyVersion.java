package net.officefloor.hq.app.settings;

/** One of a sheet's policy versions: its number (from 1, in the order made), the from-date it was recorded from
 *  (yyyy-MM-dd; null: none, in force always), and the policy setting it changed with the value it set (null: blank). */
public record PolicyVersion(int version, String from, String key, String value) {
}

package net.officefloor.hq.app.setting;

/**
 * A policy version: the from-date it is in force from (yyyy-MM-dd, or "" for none: it counts always), and the policy
 * setting its change set and the value it set it to.
 */
public record PolicyVersion(String from, String key, String value) {
}

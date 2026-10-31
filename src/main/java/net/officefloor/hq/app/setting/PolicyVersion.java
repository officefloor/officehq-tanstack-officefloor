package net.officefloor.hq.app.setting;

/** A policy version: the from-date it is in force from, the policy setting it changed and the value set ("" for none). */
public record PolicyVersion(String from, String key, String value) {
}

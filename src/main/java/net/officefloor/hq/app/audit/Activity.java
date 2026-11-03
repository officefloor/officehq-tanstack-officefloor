package net.officefloor.hq.app.audit;

import java.util.List;

/** A sheet's activity lines, "<user> <EVENT> <count>", sorted by user then event. */
public record Activity(List<String> lines) {
}

package net.officefloor.hq.app.audit;

import java.util.List;

/** A sheet's activity lines, {@code <user> <EVENT> <count>}, sorted by user then event. */
public record ActivityResponse(List<String> lines) {
}

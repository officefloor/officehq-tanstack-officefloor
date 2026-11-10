package net.officefloor.hq.app.audit;

import java.util.List;

/** A sheet's audit lines, exactly as the audit file holds them, newest first. */
public record AuditLinesResponse(List<String> lines) {
}

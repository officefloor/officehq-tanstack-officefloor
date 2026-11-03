package net.officefloor.hq.app.audit;

import java.util.List;

/** Audit lines of one sheet, exactly as the audit file holds them, newest first. */
public record AuditLines(List<String> lines) {
}

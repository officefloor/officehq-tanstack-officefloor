package net.officefloor.hq.app.opening;

import java.util.List;

/** A checked paste of opening balances: its lines in order and who makes the change. */
public record OpeningImportInput(List<OpeningLine> lines, String user) {
}

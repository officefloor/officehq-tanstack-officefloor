package net.officefloor.hq.app.opening;

import java.util.List;

/** A paste whose lines are each name,period,amount with a valid amount: who is making it, and the lines in order. */
public record OpeningImportInput(String user, List<OpeningLine> lines) {
}

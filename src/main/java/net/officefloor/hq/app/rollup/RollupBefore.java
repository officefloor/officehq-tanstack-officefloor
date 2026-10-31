package net.officefloor.hq.app.rollup;

/** The sheet's figures as they were before the change. */
public record RollupBefore(String sheetId, Snapshot snapshot) {
}

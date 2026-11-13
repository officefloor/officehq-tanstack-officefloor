package net.officefloor.hq.app.alias;

/** That the person {@code personId} is also known by {@code alias} (kept trimmed) on the sheet. */
public record Alias(String sheetId, long personId, String alias) {
}

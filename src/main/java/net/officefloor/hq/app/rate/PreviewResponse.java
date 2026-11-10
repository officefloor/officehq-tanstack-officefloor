package net.officefloor.hq.app.rate;

/** A rate preview: how many claims would show a different due, and the differences added up (two decimals). */
public record PreviewResponse(int count, String diff) {
}

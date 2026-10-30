package net.officefloor.hq.app.opening;

/** A person's opening balance for a period: the sum of the amounts pasted for them and it, with two decimals. */
public record OpeningResponse(String personId, String period, String amount) {
}

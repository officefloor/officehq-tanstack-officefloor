package net.officefloor.hq.app.opening;

/** A person's opening balance for a period as the front end reads it: the period's name, the person's id and
 *  the amount (two decimals). */
public record OpeningResponse(String period, long personId, String amount) {
}

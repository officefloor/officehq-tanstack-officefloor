package net.officefloor.hq.app.template;

/** Body of a claim template saved to the sheet: its name, amount and note, and who saves it. */
public record ClaimTemplateRequest(String name, String amount, String note, String user) {
}

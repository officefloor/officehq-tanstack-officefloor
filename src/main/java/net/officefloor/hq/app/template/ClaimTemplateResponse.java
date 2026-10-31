package net.officefloor.hq.app.template;

/** A claim template as the screen sees it: its name, amount (two decimals) and note. */
public record ClaimTemplateResponse(String name, String amount, String note) {
    static ClaimTemplateResponse from(ClaimTemplate t) {
        return new ClaimTemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

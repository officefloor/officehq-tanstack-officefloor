package net.officefloor.hq.app.template;

/** A claim template as the screen shows it (amount with two decimals). */
public record TemplateResponse(String name, String amount, String note) {
    static TemplateResponse from(ClaimTemplate t) {
        return new TemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

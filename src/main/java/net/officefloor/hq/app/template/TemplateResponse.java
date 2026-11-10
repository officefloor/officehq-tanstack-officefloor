package net.officefloor.hq.app.template;

/** A claim template, as the screen reads it: its amount with two decimals. */
public record TemplateResponse(String name, String amount, String note) {
    public static TemplateResponse from(ClaimTemplate t) {
        return new TemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

package net.officefloor.hq.app.template;

/** A claim template as sent: its amount as text with two decimals. */
public record TemplateResponse(String name, String amount, String note) {
    public static TemplateResponse from(Template t) {
        return new TemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

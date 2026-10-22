package net.officefloor.hq.app.template;

/** A template as the screen shows it, with its amount to two decimals. */
public record TemplateResponse(String name, String amount, String note, String by) {
    public static TemplateResponse from(Template t) {
        return new TemplateResponse(t.name(), t.amount().setScale(2).toPlainString(), t.note(), t.by());
    }
}

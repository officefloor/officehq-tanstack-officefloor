package net.officefloor.hq.app.template;

/** A claim template as the front end reads it: the amount with two decimals. */
public record TemplateResponse(String name, String amount, String note) {

    public static TemplateResponse from(Template t) {
        return new TemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

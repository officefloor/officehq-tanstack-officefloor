package net.officefloor.hq.app.template;

/** A claim template: its name, amount (two decimals) and note. */
public record TemplateResponse(String name, String amount, String note) {

    static TemplateResponse of(Template t) {
        return new TemplateResponse(t.name(), t.amount().toPlainString(), t.note());
    }
}

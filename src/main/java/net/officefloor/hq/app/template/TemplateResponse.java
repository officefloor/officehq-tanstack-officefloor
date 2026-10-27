package net.officefloor.hq.app.template;

/** A claim template as the screen sees it: its name, its amount with two decimals and its note. */
public record TemplateResponse(String name, String amount, String note) {

    public static TemplateResponse from(ClaimTemplate template) {
        return new TemplateResponse(template.name(), template.amount().toPlainString(), template.note());
    }
}

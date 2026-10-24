package net.officefloor.hq.app.template;

/** A claim template as the screen shows it: its name, amount (two decimals) and note. */
public record TemplateResponse(long id, String name, String amount, String note) {

    public static TemplateResponse from(ClaimTemplate template) {
        return new TemplateResponse(template.getId(), template.getName(), template.getAmount().toPlainString(),
                template.getNote());
    }
}

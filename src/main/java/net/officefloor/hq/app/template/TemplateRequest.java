package net.officefloor.hq.app.template;

/** A claim template as the screen sends it: its name, amount and note. */
public record TemplateRequest(String name, String amount, String note) {
}

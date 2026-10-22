package net.officefloor.hq.app.statement;

import java.math.BigDecimal;

/** A statement taken of a person: their name, due-total then (null: n/a) and how many of their claims counted. */
public record Statement(String sheetId, String name, BigDecimal dueTotal, int claims) {
}

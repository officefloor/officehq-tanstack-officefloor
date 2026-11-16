package net.officefloor.hq.app.rollup;

/** A person's figures as shown: their claimed and due-total (two decimals; the due-total may be n/a). */
public record Figures(String name, String claimed, String dueTotal) {
}

package net.officefloor.hq.app.rate;

/** A rate preview: how many claims would show a different due, and the sum of the differences ("3.00", "-1.50"). */
public record RatePreviewResponse(int count, String diff) {
}

package net.officefloor.hq.app.claim;

/** What a re-date submits: the claim's new date (yyyy-MM-dd; null or blank: none typed). */
public record RedateRequest(String date) {
}

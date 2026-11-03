package net.officefloor.hq.app.claim;

public record ClaimRequest(String amount, String date, String note, String corrects, String ref, String tags) {
}

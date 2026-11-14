package net.officefloor.hq.app.sheet;

/** A sheet: the id everything else in the app is kept under, and its own today (yyyy-MM-dd). */
public record Sheet(String id, String createdBy, String today) {
}

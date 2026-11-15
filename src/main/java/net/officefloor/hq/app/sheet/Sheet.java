package net.officefloor.hq.app.sheet;

/** A sheet: the id everything else in the app is kept under, its own today (yyyy-MM-dd), and whether
 *  a steward has archived it (then every change to it is refused). */
public record Sheet(String id, String createdBy, String today, boolean archived) {
}

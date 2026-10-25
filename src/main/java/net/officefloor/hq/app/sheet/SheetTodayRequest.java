package net.officefloor.hq.app.sheet;

/** The sheet's new today, and the signed-in user (`by`) making the change. */
public record SheetTodayRequest(String today, String by) {
}

package net.officefloor.hq.app.opening;

/** The opening balance paste as typed: lines of name,period,amount, and the signed-in user (`by`) making the change. */
public record ImportOpeningsRequest(String lines, String by) {
}

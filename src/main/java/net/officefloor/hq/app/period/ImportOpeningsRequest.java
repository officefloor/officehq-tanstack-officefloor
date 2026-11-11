package net.officefloor.hq.app.period;

/** Opening balances pasted into the opening import box: a line of name,period,amount each. */
public record ImportOpeningsRequest(String text) {
}

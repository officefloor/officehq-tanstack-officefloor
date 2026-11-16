package net.officefloor.hq.app.opening;

/** Body of a paste of opening balances: lines of name,period,amount, and who pastes them. */
public record ImportOpeningsRequest(String text, String by) {
}

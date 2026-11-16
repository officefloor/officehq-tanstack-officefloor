package net.officefloor.hq.app.person;

/** Body of holding a person: whether they are to be held (true) or released (false), and who does it. */
public record HoldRequest(Boolean held, String by) {
}

package net.officefloor.hq.app.person;

/** A hold once validated: whether the person is to be held (true) or released (false), and who does it. */
public record Hold(boolean held, String by) {
}

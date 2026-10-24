package net.officefloor.hq.app.person;

/** A validated hold request: whether the person is to be held (false: released). */
public record HoldInput(boolean held) {
}

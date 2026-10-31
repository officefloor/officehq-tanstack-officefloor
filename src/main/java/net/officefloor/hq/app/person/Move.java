package net.officefloor.hq.app.person;

/** A checked move: up (else down) the list, and by whom. */
public record Move(boolean up, String user) {
}

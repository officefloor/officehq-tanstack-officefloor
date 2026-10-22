package net.officefloor.hq.app.delegation;

import java.time.LocalDate;

/** A validated delegation: who is making it, the user it is for and the dates it runs, both included. */
public record DelegationInput(String user, String name, LocalDate from, LocalDate to) {
}

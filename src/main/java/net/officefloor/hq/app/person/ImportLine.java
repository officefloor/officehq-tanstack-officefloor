package net.officefloor.hq.app.person;

/** A pasted line (numbered from 1) and the person it adds, as the person form's rules leave it. */
public record ImportLine(int number, PersonRequest person) {
}

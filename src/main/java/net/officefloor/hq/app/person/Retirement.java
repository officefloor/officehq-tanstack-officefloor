package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retirement; {@code from} is null when it was left blank (the sheet's today). */
public record Retirement(LocalDate from) {
}

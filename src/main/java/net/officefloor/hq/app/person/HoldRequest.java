package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/** A hold as the row submits it: its from-date (blank: the sheet's today) and optional to-date, yyyy-mm-dd. */
public record HoldRequest(@Size(max = 30) String from, @Size(max = 30) String to) {
}

package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/** The date a person is retired from, as the row submits it (yyyy-mm-dd); blank is the sheet's today. */
public record RetireRequest(@Size(max = 30) String from) {
}

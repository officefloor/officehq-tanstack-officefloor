package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;

/** A flag as the row submits it: its reason and the date it is in force until (yyyy-mm-dd; blank: the sheet's today). */
public record FlagRequest(@Size(max = 200) String reason, @Size(max = 30) String until) {
}

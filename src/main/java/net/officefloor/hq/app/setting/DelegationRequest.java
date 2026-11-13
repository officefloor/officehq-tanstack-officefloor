package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;

/** A delegation as the form submits it: the user name, its from-date and to-date (yyyy-mm-dd; blank: the sheet's today). */
public record DelegationRequest(@Size(max = 100) String user, @Size(max = 30) String from, @Size(max = 30) String to) {
}

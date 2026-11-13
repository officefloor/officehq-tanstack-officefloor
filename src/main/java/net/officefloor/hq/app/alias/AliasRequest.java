package net.officefloor.hq.app.alias;

import jakarta.validation.constraints.Size;

/** The alias the row adds for the person; kept trimmed once validated. */
public record AliasRequest(@Size(max = 200) String alias) {
}

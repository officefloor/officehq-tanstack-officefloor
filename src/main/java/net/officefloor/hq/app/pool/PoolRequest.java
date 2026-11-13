package net.officefloor.hq.app.pool;

import jakarta.validation.constraints.Size;

/** A pool as the form submits it: its name. */
public record PoolRequest(@Size(max = 200) String name) {
}

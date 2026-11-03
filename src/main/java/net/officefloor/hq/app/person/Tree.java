package net.officefloor.hq.app.person;

import java.util.List;

/** The other sheets of a sheet's tree (see {@link PersonRepository#tree}), sorted by sheet id. */
public record Tree(List<String> sheets) {
}

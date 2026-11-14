package net.officefloor.hq.app.sheet;

import java.util.List;

/** The ids of the other sheets of a sheet's tree: its ancestors nearest first, then its descendants nearest first. */
public record Tree(List<String> ids) {
}

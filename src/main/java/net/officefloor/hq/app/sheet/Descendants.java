package net.officefloor.hq.app.sheet;

import java.util.List;

/** A sheet's descendants' ids: its children, their children and so on, nearest first. */
public record Descendants(List<String> ids) {
}

package net.officefloor.hq.app.settings;

import java.util.List;

/** A sheet's descendants (see {@link SettingsRepository#descendants}), nearer first. */
public record Descendants(List<String> sheets) {
}

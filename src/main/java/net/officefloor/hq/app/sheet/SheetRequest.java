package net.officefloor.hq.app.sheet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SheetRequest(@NotBlank @Size(max = 100) String id) {
}

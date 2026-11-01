package net.officefloor.hq.app.period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/** How many periods a generate made, and (kept off the response) the names of those it made. */
public record GeneratePeriodsResult(int generated, @JsonIgnore List<String> names) {
    public GeneratePeriodsResult(int generated) {
        this(generated, List.of());
    }
}

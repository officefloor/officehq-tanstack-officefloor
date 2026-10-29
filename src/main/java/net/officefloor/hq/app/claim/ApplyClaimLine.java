package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyClaimLine {

    private static final Pattern QTY = Pattern.compile("\\d{1,9}");

    private static final Pattern UNIT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    // A line has a label, a whole quantity of at least 1 and a unit amount in up to two decimals. Adding one edits the
    // claim: its amount becomes its lines added up, and the user is kept as its last editor. A correction has no lines.
    public void apply(@RequestBody(required = false) ClaimLineRequest body, @Val Claim claim, Claims claims,
            ServerHttpConnection connection, Out<ClaimLine> lineOut, Out<ClaimResponse> responseOut)
            throws ClaimLineRefusedException {
        String label = body == null || body.label() == null ? "" : body.label().trim();
        String qty = body == null || body.qty() == null ? "" : body.qty().trim();
        String unit = body == null || body.unit() == null ? "" : body.unit().trim();
        if (label.isEmpty()) {
            throw new ClaimLineRefusedException("A line needs a label.");
        }
        if (!QTY.matcher(qty).matches() || Integer.parseInt(qty) < 1) {
            throw new ClaimLineRefusedException("A line's quantity must be a whole number, at least 1.");
        }
        if (!UNIT.matcher(unit).matches()) {
            throw new ClaimLineRefusedException("A line's unit amount must be a number with up to two decimals.");
        }
        if (claim.isCorrection()) {
            throw new ClaimLineRefusedException("A correction cannot have lines.");
        }
        ClaimLine line = new ClaimLine(label, Integer.parseInt(qty), new BigDecimal(unit).setScale(2));
        claims.addLine(claim, line);
        List<ClaimLine> lines = claims.lines(claim);
        Claim edited = claim.with(new ClaimInput(ClaimLines.total(lines), claim.date(), claim.note(), claim.corrects(),
                claim.ref(), claim.tags()), claim.date());
        claims.save(edited, Audit.user(connection).trim());
        lineOut.set(line);
        responseOut.set(ClaimResponse.from(edited).withLines(lines.stream().map(ClaimLine::shown).toList()));
    }
}

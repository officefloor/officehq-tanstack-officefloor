import type { Claim } from './claims';
import { useSelectedClaims } from './selection';

// The checkbox that ticks a claim for a bulk action (BulkOpen, BulkVoid and the others).
export function ClaimSelect({ claim }: { claim: Claim }) {
  const [selected, set] = useSelectedClaims();
  return (
    <input
      type="checkbox"
      data-testid="claim-select"
      checked={selected.has(claim.id)}
      onChange={(e) => set(claim.id, e.target.checked)}
    />
  );
}

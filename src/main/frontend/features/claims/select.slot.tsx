import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';
import { useSelectedClaims } from './useSelectedClaims';

/** The checkbox that ticks a claim for a bulk action. */
function ClaimSelect({ claim }: { sheetId: string; claim: Claim }) {
  const [selected, setSelected] = useSelectedClaims();
  const checked = selected.includes(claim.id);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={checked}
        onChange={(e) =>
          setSelected(e.target.checked ? [...selected, claim.id] : selected.filter((id) => id !== claim.id))
        }
      />
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 0, Component: ClaimSelect });

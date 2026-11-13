import { useSheetToday } from '../session/sheetToday';
import type { Claim } from './claims';

// Whether a claim is deferred on the sheet's today — it was deferred until a date after it, and stays so shown once
// voided — and the date it was last deferred until (blank when it never was). Only an OPEN one is left out of the
// totals (`deferred` in claims.ts).
export function ClaimDeferred({ claim }: { claim: Claim }) {
  const today = useSheetToday();
  const is = claim.deferredUntil !== null && today !== '' && today < claim.deferredUntil;
  return (
    <>
      <span data-testid="claim-deferred">{is ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>
    </>
  );
}

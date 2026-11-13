import { useSheetToday } from '../session/sheetToday';
import { deferred } from './claims';
import type { Claim } from './claims';

// Whether a claim is deferred on the sheet's today, and the date it was last deferred until (blank when it never was).
export function ClaimDeferred({ claim }: { claim: Claim }) {
  const today = useSheetToday();
  const is = deferred(claim, today);
  return (
    <>
      <span data-testid="claim-deferred">{is ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>
    </>
  );
}

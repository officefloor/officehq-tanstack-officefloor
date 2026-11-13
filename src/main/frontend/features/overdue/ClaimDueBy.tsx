import type { Claim } from '../claims/claims';

// A claim's due-by: fixed when it was created, from the terms of its person's grade in force on its date.
export function ClaimDueBy({ claim }: { claim: Claim }) {
  return <span data-testid="claim-due-by">{claim.dueBy}</span>;
}

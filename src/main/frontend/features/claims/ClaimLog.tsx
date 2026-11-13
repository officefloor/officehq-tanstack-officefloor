import type { Claim } from './claims';

// The claim's status log, oldest first: one line per status it has had.
export function ClaimLog({ claim }: { claim: Claim }) {
  return (
    <ol>
      {(claim.log ?? []).map((l, i) => (
        <li key={i} data-testid="claim-log-line">
          {`${l.status} by ${l.by} on ${l.date} due=${l.due}`}
        </li>
      ))}
    </ol>
  );
}

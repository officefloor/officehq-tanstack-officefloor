import { ClaimCells } from '../../slots/defs/claimRow';

// A claim's status log: one line per status it has had, oldest first, as `<STATUS> by <user> on <date> due=<due>`
// (the sheet's today when it changed, and its due as computed then).
function ClaimLog({ claim }: { sheetId: string; user: string; personId: number; claim: { log?: string[] } }) {
  return (
    <ol data-testid="claim-log">
      {(claim.log ?? []).map((line, i) => (
        <li key={i} data-testid="claim-log-line">{line}</li>
      ))}
    </ol>
  );
}

export const contribution = ClaimCells.fill({ order: 40, Component: ClaimLog });

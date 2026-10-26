import { ClaimCells } from '../../slots/defs/claimRow';

// A claim's lines, in the order added, as `<label> <qty> x <unit>`; with lines, its amount is their total.
function ClaimLines({ claim }: { sheetId: string; user: string; personId: number; claim: { lines?: string[] } }) {
  return (
    <ol data-testid="claim-lines">
      {(claim.lines ?? []).map((line, i) => (
        <li key={i} data-testid="claim-lines-line">{line}</li>
      ))}
    </ol>
  );
}

export const contribution = ClaimCells.fill({ order: 46, Component: ClaimLines });

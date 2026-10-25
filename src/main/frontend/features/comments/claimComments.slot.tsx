import { ClaimCells } from '../../slots/defs/claimRow';

// A claim's comments, oldest first, as `<user> on <date>: <text>` (the sheet's today when it was written).
function ClaimComments({ claim }: { sheetId: string; user: string; personId: number; claim: { comments?: string[] } }) {
  return (
    <ol data-testid="claim-comments">
      {(claim.comments ?? []).map((line, i) => (
        <li key={i} data-testid="claim-comments-line">{line}</li>
      ))}
    </ol>
  );
}

export const contribution = ClaimCells.fill({ order: 45, Component: ClaimComments });

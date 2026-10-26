import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';

// A claim's timeline: each press reads the audit lines about it, newest first, as the file holds them — those
// carrying its number, or for a claim with no number those naming its person, amount and date. It records nothing.
function ClaimTimeline({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { id: number } }) {
  const show = useMutation({
    mutationFn: () =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/timeline`),
  });

  return (
    <span>
      <button
        type="button"
        data-testid="claim-timeline"
        disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate();
        }}
      >
        Timeline
      </button>
      {show.isError && (
        <span data-testid="claim-timeline-error" role="alert">
          {' '}
          {show.error.message}
        </span>
      )}
      {show.data && (
        <ol data-testid="claim-timeline-list">
          {show.data.map((line, i) => (
            <li key={i} data-testid="claim-timeline-line">{line}</li>
          ))}
        </ol>
      )}
    </span>
  );
}

export const contribution = ClaimCells.fill({ order: 47, Component: ClaimTimeline });

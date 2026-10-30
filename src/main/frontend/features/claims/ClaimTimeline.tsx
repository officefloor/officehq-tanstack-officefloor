import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// A claim's timeline: the audit lines about it exactly as the file holds them, newest first — those carrying its
// number, or for a claim with no number those naming its person, amount and date together. It reads the file on
// each show; a new show clears the old list first.
export function ClaimTimeline({ claimId }: { claimId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const show = useMutation({
    mutationFn: () =>
      getJson<string[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(claimId)}/timeline`,
      ),
  });
  return (
    <>
      <button
        type="button"
        data-testid="claim-timeline"
        onClick={() => {
          show.reset();
          show.mutate();
        }}
      >
        Timeline
      </button>
      {show.isError && <span data-testid="claim-timeline-error" role="alert"> Could not read the timeline.</span>}
      <ol data-testid="claim-timeline-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="claim-timeline-line">
            {line}
          </li>
        ))}
      </ol>
    </>
  );
}

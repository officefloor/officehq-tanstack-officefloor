import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/**
 * A claim's timeline: the audit lines about it, newest first, as the audit file holds them — the
 * lines carrying its number, or, for a claim with no number, those naming its person, amount and
 * date together. Each press re-reads the file; it is not a second record.
 */
export function ClaimTimeline({ sheet, claimId }: { sheet: string; claimId: number }) {
  const show = useMutation({
    mutationFn: () =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claimId}/timeline`),
  });

  return (
    <>
      <button
        type="button"
        data-testid="claim-timeline"
        disabled={show.isPending}
        onClick={() => {
          // The old lines go while the new ones are read, so two presses are told apart.
          show.reset();
          show.mutate();
        }}
      >
        Timeline
      </button>
      {show.isError && (
        <span data-testid="claim-timeline-error" role="alert">
          {' '}
          Could not read the timeline.
        </span>
      )}
      <ol data-testid="claim-timeline-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="claim-timeline-line">
            <code>{line}</code>
          </li>
        ))}
      </ol>
    </>
  );
}

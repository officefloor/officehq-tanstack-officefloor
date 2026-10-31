import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The sheet's activity: for every user and every event in the sheet's audit lines, one line
// "<user> <EVENT> <count>", sorted by user then event. It reads the file on each show; a new show clears
// the old list first.
export function ActivityPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/activity`),
  });
  return (
    <section>
      <button
        type="button"
        data-testid="activity-show"
        onClick={() => {
          show.reset();
          show.mutate();
        }}
      >
        Show activity
      </button>
      {show.isError && <span data-testid="activity-error" role="alert"> Could not read the activity.</span>}
      <ul data-testid="activity-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="activity-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}

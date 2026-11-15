import { useMutation } from '@tanstack/react-query';
import { listActivity } from './api';

/** The sheet's activity, read on demand from its audit file: how many records each user made of each
 *  event, sorted by user then event. */
export function ActivityViewer({ sheetId }: { sheetId: string; user: string }) {
  const show = useMutation({ mutationFn: () => listActivity(sheetId) });

  return (
    <section>
      <h3>Activity</h3>
      <button type="button" data-testid="activity-show" disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate();
        }}>
        Show
      </button>
      {show.isError && <span data-testid="activity-error">The activity could not be read.</span>}
      <ul>
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="activity-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}

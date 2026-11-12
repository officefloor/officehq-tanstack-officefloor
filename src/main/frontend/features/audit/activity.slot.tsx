import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';

// The sheet's activity: for every user and every event in its audit lines, one line
// "<user> <EVENT> <count>", sorted by user then event. It reads the file each time Show is pressed.
function Activity() {
  const [sheet] = useCurrentSheet();

  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/activity`),
  });

  return (
    <section data-testid="activity">
      <h2>Activity</h2>
      <button type="button" data-testid="activity-show" disabled={sheet === ''} onClick={() => show.mutate()}>
        Show
      </button>
      {show.isError && (
        <span data-testid="activity-error" role="alert">
          {' '}
          The activity could not be read.
        </span>
      )}
      <ul>
        {(show.isPending ? [] : (show.data ?? [])).map((line) => (
          <li key={line} data-testid="activity-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 53, Component: Activity });

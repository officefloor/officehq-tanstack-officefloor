import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';

/** The sheet's activity: for every user and event in its audit file, one line <user> <EVENT> <count>, sorted by user
 *  then event; read when Show is pressed. */
function Activity({ sheetId }: { sheetId: string }) {
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/activity`),
  });
  return (
    <section data-testid="activity-view">
      <h3>Activity</h3>
      <button
        type="button"
        data-testid="activity-show"
        disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate();
        }}
      >
        Show
      </button>
      {show.isError && <p data-testid="activity-error">Could not read the activity.</p>}
      <ul>
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="activity-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 91, Component: Activity });

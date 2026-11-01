import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';

/**
 * The sheet's activity: for every user and every event in the sheet's audit file, one line
 * `<user> <EVENT> <count>`, sorted by user then event. It reads the file; it records nothing.
 */
function ActivityPanel({ sheet }: { user: string; sheet: string }) {
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/activity`),
  });

  return (
    <section data-testid="activity-panel">
      <h2>Activity</h2>
      <button
        type="button"
        data-testid="activity-show"
        disabled={show.isPending}
        onClick={() => {
          // The old lines go while the new ones are read, so two presses are told apart.
          show.reset();
          show.mutate();
        }}
      >
        Show
      </button>
      {show.isError && (
        <span data-testid="activity-error" role="alert">
          {' '}
          Could not read the audit file.
        </span>
      )}
      <ol data-testid="activity-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="activity-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28.5, Component: ActivityPanel });

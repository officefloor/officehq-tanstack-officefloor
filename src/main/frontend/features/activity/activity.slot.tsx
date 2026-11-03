import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';

/** The sheet's activity: for every user and event in its audit lines, "<user> <EVENT> <count>", sorted
 *  by user then event. It reads the audit file; it records nothing. */
function ActivityViewer({ sheetId }: { sheetId: string }) {
  const show = useMutation({
    mutationFn: () => getJson<{ lines: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/activity`),
  });

  return (
    <section data-testid="activity">
      <h2>Activity</h2>
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
      {show.isError && <span data-testid="activity-error">{show.error.message}</span>}
      {show.isSuccess && (
        <ul>
          {show.data.lines.map((line, i) => (
            <li key={i} data-testid="activity-line">
              {line}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 91,
  Component: ({ sheetId }) => <ActivityViewer sheetId={sheetId} />,
});

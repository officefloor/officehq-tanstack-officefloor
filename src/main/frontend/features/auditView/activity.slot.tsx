import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';

/** The sheet's activity: one line "<user> <EVENT> <count>" per user and event in its audit lines, sorted by user then
 *  event. Re-read from the audit file each time it is shown. */
function Activity({ sheetId }: { sheetId: string }) {
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');

  const show = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/activity`),
    onSuccess: (found) => setLines(found),
    onError: (e: Error) => setError(e.message || 'The activity could not be read.'),
  });

  return (
    <section data-testid="activity">
      <h3>Activity</h3>
      <button
        type="button"
        data-testid="activity-show"
        disabled={show.isPending}
        onClick={() => {
          setError('');
          setLines([]);
          show.mutate();
        }}
      >
        Show
      </button>
      {error && <p data-testid="activity-error">{error}</p>}
      <ul data-testid="activity-lines">
        {lines.map((line) => (
          <li key={line} data-testid="activity-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so the lines shown never carry over to another sheet.
export const contribution = SheetPanel.fill({
  order: 40.5,
  Component: ({ sheetId }) => <Activity key={`activity-${sheetId}`} sheetId={sheetId} />,
});

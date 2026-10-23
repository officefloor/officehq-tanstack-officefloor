import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';

/** The sheet's activity: for every user and every event in the sheet's audit file, "<user> <EVENT> <count>",
 *  sorted by user then event. Each press of Show re-reads the file. */
function ActivityViewer({ sheetId }: { sheetId: string; user: string }) {
  const [shown, setShown] = useState(0);
  const lines = useQuery({
    queryKey: ['sheet', sheetId, 'activity', shown],
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/activity`),
    enabled: shown > 0,
  });

  return (
    <section>
      <h2>Activity</h2>
      <button type="button" data-testid="activity-show" onClick={() => setShown((n) => n + 1)}>
        Show
      </button>
      {shown > 0 && lines.data && (
        <ol>
          {lines.data.map((line, i) => (
            <li key={i} data-testid="activity-line">
              {line}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 91, Component: ActivityViewer });

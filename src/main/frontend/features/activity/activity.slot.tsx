import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';

// The sheet's activity: a show button and, for every user and every event in the sheet's audit
// file, one line "<user> <EVENT> <count>", sorted by user then event. It writes nothing.
function Activity({ sheetId }: { sheetId: string }) {
  const [user] = useUser();
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');

  const show = useMutation({
    mutationFn: () =>
      getJson<{ lines: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/activity`, user),
    onSuccess: (data) => setLines(data.lines),
    onError: () => setError('The activity could not be read.'),
  });

  return (
    <section data-testid="activity">
      <h2>Activity</h2>
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
      {error && (
        <p role="alert" data-testid="activity-error">
          {error}
        </p>
      )}
      <ul data-testid="activity-lines">
        {lines.map((line, i) => (
          <li key={i} data-testid="activity-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 41, Component: Activity });

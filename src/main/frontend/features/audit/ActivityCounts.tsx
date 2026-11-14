import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The sheet's activity: one line "<user> <EVENT> <count>" for every user and event in its audit lines, sorted by
// user then event. Read on each press of the show button; the old lines are gone while it reads.
export function ActivityCounts() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [lines, setLines] = useState<string[]>([]);
  const pressed = useRef(0);

  const show = useMutation({
    mutationFn: async (press: number) => ({
      press,
      read: await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/activity`),
    }),
    // Only the latest press's answer is shown; an earlier one that arrives late is dropped.
    onSuccess: ({ press, read }) => {
      if (press === pressed.current) setLines(read);
    },
  });

  if (!user || !sheet) return null;

  const press = () => {
    setLines([]);
    pressed.current += 1;
    show.mutate(pressed.current);
  };

  return (
    <section data-testid="activity">
      <button type="button" data-testid="activity-show" onClick={press}>
        Show activity
      </button>
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

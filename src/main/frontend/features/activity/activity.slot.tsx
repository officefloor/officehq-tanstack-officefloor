import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';

const activityPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/activity`;

// The sheet's activity: one line "<user> <EVENT> <count>" per user and event in the sheet's audit file, sorted
// by user then event. It reads the file each time Show is pressed; while a read runs the old lines are gone.
function Activity() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const show = useMutation({
    mutationFn: () => getJson<string[]>(activityPath(sheet ?? '')),
  });

  if (!user || !sheet) return null;
  return (
    <section data-testid="activity">
      <h2>Activity</h2>
      <button type="button" data-testid="activity-show" onClick={() => show.mutate()}>Show</button>
      <ul>
        {(show.isPending ? [] : show.data ?? []).map((line, i) => (
          <li key={i} data-testid="activity-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 100, Component: Activity });

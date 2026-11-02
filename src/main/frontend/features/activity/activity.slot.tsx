import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useActivity } from './activity';

// The sheet's activity: how many of each event every user has in the sheet's audit file, by user then event. It reads
// the file as it stands at each press; while a press is being read the old lines are gone.
function Activity({ sheet }: { sheet: string; user: string }) {
  const [press, setPress] = useState(0);
  const { data: lines, isSuccess, isFetching, isError } = useActivity(sheet, press);

  return (
    <section>
      <h2>Activity</h2>
      <button type="button" data-testid="activity-show" onClick={() => setPress((p) => p + 1)}>
        Show
      </button>
      {isError && (
        <span data-testid="activity-error" role="alert">
          {' '}The activity could not be read.
        </span>
      )}
      {isSuccess && !isFetching && (
        <ul>
          {lines.map((line) => (
            <li key={line} data-testid="activity-line">
              <code>{line}</code>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 101, Component: Activity });

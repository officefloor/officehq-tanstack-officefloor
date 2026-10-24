import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { PEOPLE_COLUMNS, useHiddenColumns } from './columns';

// A checkbox per switchable column of the people list, for this user's screen only.
function ColumnChooser() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [off, setShown] = useHiddenColumns();
  if (!user || !sheet) return null;
  return (
    <fieldset>
      <legend>Columns</legend>
      {PEOPLE_COLUMNS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            data-testid={`column-${c}`}
            checked={!off.has(c)}
            onChange={(e) => setShown(c, e.target.checked)}
          />{' '}
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

export const contribution = HomeMain.fill({ order: 18.5, Component: ColumnChooser });

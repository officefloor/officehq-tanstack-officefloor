import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useShowRetired } from './people';

// The show-retired toggle above the people list: off, retired people have no row at all.
function ShowRetiredToggle() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [showRetired, setShowRetired] = useShowRetired();
  if (!user || !sheet) return null;
  return (
    <label>
      <input
        type="checkbox"
        data-testid="show-retired"
        checked={showRetired}
        onChange={(e) => setShowRetired(e.target.checked || undefined)}
      />{' '}
      Show retired
    </label>
  );
}

export const contribution = HomeMain.fill({ order: 18, Component: ShowRetiredToggle });

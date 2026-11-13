import { useShowRetired } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The toggle that lists retired people too; off when the screen is first opened.
export function ShowRetired() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [on, setOn] = useShowRetired();

  if (!user || !sheet) return null;

  return (
    <label>
      <input
        type="checkbox"
        data-testid="show-retired"
        checked={on}
        onChange={(e) => setOn(e.target.checked || undefined)}
      />{' '}
      Show retired
    </label>
  );
}

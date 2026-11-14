import { asFlag, useSearchParam } from '../../url/useSearchParam';

/** Whether retired people are listed. It lives in the URL (`retired`): off when the screen is first
 *  opened, and left as it is by loading a sheet. */
export const useShowRetired = () => useSearchParam('retired', asFlag);

/** The "show retired" toggle. */
export function ShowRetired() {
  const [on, setOn] = useShowRetired();
  return (
    <label>
      <input type="checkbox" data-testid="show-retired" checked={on} onChange={(e) => setOn(e.target.checked || undefined)} />{' '}
      Show retired
    </label>
  );
}

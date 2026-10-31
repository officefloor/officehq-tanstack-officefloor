import { asFlag, useSearchParam } from '../../url/useSearchParam';

/** Whether retired people are listed. Off when the screen is first opened; kept in the URL, so
 *  loading a sheet leaves it as it is. */
export function useShowRetired() {
  return useSearchParam('retired', asFlag);
}

/** The "show retired" toggle above the people list. */
export function ShowRetired() {
  const [on, setOn] = useShowRetired();
  return (
    <label>
      <input
        type="checkbox"
        data-testid="show-retired"
        checked={on}
        onChange={(e) => setOn(e.target.checked ? true : undefined)}
      />{' '}
      Show retired
    </label>
  );
}

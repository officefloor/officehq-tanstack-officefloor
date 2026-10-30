import { asFlag, useSearchParam } from '../../url/useSearchParam';

// The "show retired" toggle: off when the screen is first opened; it lives in the URL
// (`showRetired`), so loading a sheet leaves it as it is.
export function useShowRetired(): [boolean, (on: boolean) => void] {
  const [on, setOn] = useSearchParam('showRetired', asFlag);
  return [on, (value) => setOn(value ? true : undefined)];
}

export function ShowRetired() {
  const [on, setOn] = useShowRetired();
  return (
    <label>
      <input type="checkbox" data-testid="show-retired" checked={on} onChange={(e) => setOn(e.target.checked)} /> Show
      retired
    </label>
  );
}

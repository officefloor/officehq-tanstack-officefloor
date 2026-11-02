import { asFlag, useSearchParam } from '../../url/useSearchParam';

/** Whether retired people are listed (URL key `showRetired`): off until turned on, and left as it is
 *  by loading a sheet. */
export function useShowRetired(): [boolean, (on: boolean) => void] {
  const [on, setOn] = useSearchParam('showRetired', asFlag);
  return [on, (v) => setOn(v ? true : undefined)];
}

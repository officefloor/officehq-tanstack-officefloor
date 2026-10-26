import { asFlag, useSearchParam } from '../../url/useSearchParam';

/** Whether retired people are listed, kept in the URL: off when the screen is first opened, and left as it is by loading a sheet. */
export function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}

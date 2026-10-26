import { asString, useSearchParam } from '../../url/useSearchParam';

/** The id of the active sheet, kept in the URL. */
export function useSheetId() {
  return useSearchParam('sheet', asString);
}

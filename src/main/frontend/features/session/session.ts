import { asString, useSearchParam } from '../../url/useSearchParam';

/** The signed-in name (no password) — kept in the URL like any other state that outlives a click. */
export function useUser() {
  return useSearchParam('user', asString);
}

/** The active sheet id. */
export function useSheetId() {
  return useSearchParam('sheet', asString);
}

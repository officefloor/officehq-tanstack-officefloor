import { asString, useSearchParam } from '../../url/useSearchParam';

// The signed-in user and the active sheet live in the URL (search params `user` and `sheet`), so
// each browser tab is its own session and every feature reads them directly.
export function useUser() {
  return useSearchParam('user', asString);
}

export function useSheetId() {
  return useSearchParam('sheet', asString);
}

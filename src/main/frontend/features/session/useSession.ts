import { asString, useSearchParam } from '../../url/useSearchParam';

/**
 * Who is signed in and which sheet is active. Both live in the URL (`user`, `sheet`) like any other
 * state that outlives a click; every feature that needs them reads them from here.
 */
export function useCurrentUser() {
  return useSearchParam('user', asString);
}

export function useCurrentSheet() {
  return useSearchParam('sheet', asString);
}

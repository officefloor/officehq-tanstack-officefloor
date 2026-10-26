import { asString, useSearchParam } from '../../url/useSearchParam';

/** The signed-in name. There is no password: signing in is typing a name, kept in the URL. */
export function useUser() {
  return useSearchParam('user', asString);
}

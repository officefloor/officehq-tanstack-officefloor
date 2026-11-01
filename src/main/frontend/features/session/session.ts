import { asString, useSearchParam } from '../../url/useSearchParam';

// Who is signed in and which sheet is active. Both live in the URL (search params `user` and
// `sheet`), so any feature reads them directly.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

/** A write's path carrying who made it, so the server can record it in the audit file. */
export const byUser = (path: string, user: string) => `${path}?user=${encodeURIComponent(user)}`;

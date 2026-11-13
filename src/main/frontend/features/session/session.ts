import { asString, useSearchParam } from '../../url/useSearchParam';

// The session lives in the URL: `user` is the signed-in name, `sheet` the active sheet id. Each
// browser keeps its own, so two users on one sheet act independently.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

/** The query key under which everything kept per sheet lives; re-loading a sheet invalidates it. */
export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

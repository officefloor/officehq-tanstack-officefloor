import { asString, useSearchParam } from '../../url/useSearchParam';

// The session lives in the URL: `user` is the signed-in name, `sheet` the active sheet id. Each
// browser keeps its own, so two users on one sheet act independently.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

/** The query key under which everything kept per sheet lives; re-loading a sheet invalidates it. */
export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

/** A sheet as the store holds it: its id and its own "today" (yyyy-MM-dd). */
export type SheetInfo = { id: string; today: string };

/** The query key for the sheet itself; under sheetKey, so re-loading the sheet re-reads it. */
export const sheetInfoKey = (sheet: string) => [...sheetKey(sheet), 'info'] as const;

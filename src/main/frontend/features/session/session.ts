import { asString, useSearchParam } from '../../url/useSearchParam';

// The session lives in the URL: `user` is the signed-in name (no password), `sheet` the active
// sheet id. Every feature that works per sheet or records who did something reads these.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

export type Sheet = { id: string };
export const sheetKey = (id: string) => ['sheet', id] as const;

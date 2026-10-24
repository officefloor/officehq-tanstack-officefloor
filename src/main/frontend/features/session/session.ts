import { asString, useSearchParam } from '../../url/useSearchParam';

// The signed-in name and the active sheet id live in the URL (search params `user` and `sheet`).
// Everything else in the app is kept per sheet: a feature reads the sheet id from here and keys
// its queries under ['sheet', sheetId, ...], so loading a sheet re-reads all of them.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

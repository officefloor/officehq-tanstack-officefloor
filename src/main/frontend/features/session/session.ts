import { asString, useSearchParam } from '../../url/useSearchParam';

// Who is signed in and which sheet is active. Both live in the URL (`user`, `sheet`), so any
// feature reads them directly and a browser refresh keeps them.
export const useUser = () => useSearchParam('user', asString);
export const useSheetId = () => useSearchParam('sheet', asString);

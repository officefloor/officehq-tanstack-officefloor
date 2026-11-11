import { asString, useSearchParam } from '../url/useSearchParam';

// The signed-in name and the active sheet id live in the URL (`user`, `sheet`), so every feature
// reads them directly and each browser tab is its own session.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

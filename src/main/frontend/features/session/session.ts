import { asString, useSearchParam } from '../../url/useSearchParam';

// The session lives in the URL: `user` is the signed-in name, `sheet` the active sheet id.
export const useUser = () => useSearchParam('user', asString);
export const useSheetId = () => useSearchParam('sheet', asString);

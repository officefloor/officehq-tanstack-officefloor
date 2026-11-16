import { createContext, useContext } from 'react';

/** Whether the reads below ask for the sheet as it is now, whatever as-of date the screen shows (see features/asof):
 *  the sheet, its people, claims, rates and settings are read so. Off everywhere but where a feature turns it on. */
export const ReadNow = createContext(false);

export const useReadNow = () => useContext(ReadNow);

/** The key a read is kept under: a read of the sheet as it is now is kept apart (under the same prefix, so whatever
 *  invalidates the sheet's own read invalidates it too). */
export const nowKey = (key: unknown[], now: boolean) => (now ? [...key, 'now'] : key);

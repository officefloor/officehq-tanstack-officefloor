import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A role given to a user name: clerk or steward. A name with none given is a clerk. */
export type UserRole = { user: string; role: string };

/**
 * The sheet's settings as the store holds them: the clerk limit (two decimals; null: no limit), above
 * which a clerk cannot settle a claim, and the roles given to user names.
 */
export type Settings = { clerkLimit: string | null; roles: UserRole[] };

/** The roles a user name can be given. */
export const ROLES = ['clerk', 'steward'];

/** 0.00 or more, with at most two decimals. */
export const AMOUNT = /^(\d{1,13}(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's settings; under sheetKey, so re-loading the sheet re-reads them. */
export const settingsKey = (sheet: string) => [...sheetKey(sheet), 'settings'] as const;

/** The sheet's settings. */
export function useSettings() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: !!sheet,
  });
}

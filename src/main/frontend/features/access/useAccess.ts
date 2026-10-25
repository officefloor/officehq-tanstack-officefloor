import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { toHundredths } from '../rates/useRates';

/** A role assigned to a user name: clerk (any name without one) or steward. */
export type Role = { user: string; role: string };

/** A sheet setting; a null value is blank. */
export type Setting = { key: string; value: string | null };

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;
export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

/** The sheet's assigned roles, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  });
}

/** The floor a sheet starts with, until it is set. */
export const FLOOR_DEFAULT = '1.00';

/** The sheet's settings, by key. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The sheet's floor in cents: the least a claim's live due may be (1.00 until it is set). */
export function useFloorCents(sheetId: string): bigint {
  const value = useSettings(sheetId).data?.find((s) => s.key === 'floor')?.value ?? FLOOR_DEFAULT;
  return toHundredths(value);
}

/** The terms a sheet starts with, until they are set: the days after its date a claim is due by. */
export const TERMS_DEFAULT = '14';

/** The sheet's terms in days: a claim is due by its date plus these (14 until they are set). */
export function useTermsDays(sheetId: string): number {
  const value = useSettings(sheetId).data?.find((s) => s.key === 'terms')?.value ?? TERMS_DEFAULT;
  return Number(value);
}

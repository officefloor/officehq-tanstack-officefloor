import type { QueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { periodsKey } from '../periods/periods';
import type { Period } from '../periods/periods';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { poolsKey } from '../pools/pools';
import type { Pool } from '../pools/pools';
import { ratesKey } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetInfoKey } from '../session/session';
import type { SheetInfo } from '../session/session';
import { settingsKey } from '../settings/settings';
import type { Settings } from '../settings/settings';

/**
 * One part of the sheet re-read from the store under its usual query key, so an export never builds
 * from what the screen last held: while the screen is shown as of a date (the read carries it), the
 * export takes the as-of figures even when the screen has not yet re-read them.
 */
function fresh<T>(queryClient: QueryClient, queryKey: readonly unknown[], path: string): Promise<T> {
  return queryClient.fetchQuery({ queryKey, queryFn: () => getJson<T>(path), staleTime: 0 });
}

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}`;

export const freshSheetInfo = (qc: QueryClient, sheet: string) => fresh<SheetInfo>(qc, sheetInfoKey(sheet), base(sheet));
export const freshPeople = (qc: QueryClient, sheet: string) => fresh<Person[]>(qc, peopleKey(sheet), `${base(sheet)}/people`);
export const freshClaims = (qc: QueryClient, sheet: string) => fresh<Claim[]>(qc, claimsKey(sheet), `${base(sheet)}/claims`);
export const freshRates = (qc: QueryClient, sheet: string) => fresh<Rate[]>(qc, ratesKey(sheet), `${base(sheet)}/rates`);
export const freshPeriods = (qc: QueryClient, sheet: string) => fresh<Period[]>(qc, periodsKey(sheet), `${base(sheet)}/periods`);
export const freshSettings = (qc: QueryClient, sheet: string) => fresh<Settings>(qc, settingsKey(sheet), `${base(sheet)}/settings`);
export const freshPools = (qc: QueryClient, sheet: string) => fresh<Pool[]>(qc, poolsKey(sheet), `${base(sheet)}/pools`);

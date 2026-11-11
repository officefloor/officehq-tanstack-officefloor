import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/**
 * A policy version, oldest first: its number (from 1), its from-date (yyyy-MM-dd; '' for none, counting
 * always), the policy setting it changed and the value it set, and the count of claims whose policy
 * version it is.
 */
export type PolicyVersion = { version: number; from: string; key: string; value: string; claims: number };

export const policyKey = (sheetId: string, stamp: number) => ['sheet', sheetId, 'policy', stamp] as const;

/** The sheet's policy versions, read again whenever `stamp` (the time its claims or settings were last read) moves. */
export function usePolicyVersions(sheetId: string, stamp: number) {
  return useQuery({
    queryKey: policyKey(sheetId, stamp),
    queryFn: async () =>
      (await getJson<{ versions: PolicyVersion[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/policy`)).versions,
    placeholderData: (previous) => previous,
  });
}

/** A policy version as one line: <version> <from or start> <key>=<value> <count of claims>. */
export const policyLine = (v: PolicyVersion) => `${v.version} ${v.from || 'start'} ${v.key}=${v.value} ${v.claims}`;

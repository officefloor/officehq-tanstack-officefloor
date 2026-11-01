import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** One press of the viewer's show button: the filter it was pressed with, and which press it was (so two presses
 *  with the same filter read the file again). */
export type AuditShown = { name: string; press: number };

export const auditKey = (sheet: string, shown: AuditShown | null) => ['audit', sheet, shown] as const;

export const auditPath = (sheet: string, name: string) =>
  `/api/sheets/${encodeURIComponent(sheet)}/audit?name=${encodeURIComponent(name)}`;

/** The sheet's audit lines exactly as the file holds them, newest first; with a name, only those about that person.
 *  Nothing is read until the viewer's show button is pressed. */
export function useAuditLines(sheet: string, shown: AuditShown | null) {
  return useQuery({
    queryKey: auditKey(sheet, shown),
    queryFn: () => getJson<string[]>(auditPath(sheet, shown?.name ?? '')),
    enabled: shown !== null,
    gcTime: 0,
  });
}

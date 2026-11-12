import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A member of a crew: a person on the sheet. */
export type CrewMember = { personId: number; name: string };

/** A crew on the sheet: its lead (`lead` null once the person is no longer on the sheet) and its members. */
export type Crew = { id: number; name: string; leadId: number; lead: string | null; members: CrewMember[] };

/** Under the sheet's key, so loading the sheet again re-reads its crews. */
export const crewsKey = (sheet: string) => [...sheetKey(sheet), 'crews'] as const;

export function useCrews(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(crewsKey(sheet), asOf),
    queryFn: () => getJson<Crew[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/crews`, asOf)),
    enabled: sheet !== '',
  });
}

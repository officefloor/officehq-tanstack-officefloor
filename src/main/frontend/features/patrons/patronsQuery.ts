import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** One link: the person (`personId`) names another person on the sheet (`patronId`) as their patron. */
export type PatronLink = { personId: number; patronId: number };

/** Under the sheet's key, so re-loading the sheet re-reads its patrons. */
export const patronsKey = (sheetId: string) => [...sheetKey(sheetId), 'patrons'] as const;

export const patronsQuery = (sheetId: string) => ({
  queryKey: patronsKey(sheetId),
  queryFn: () => getJson<PatronLink[]>(`/api/sheets/${encodeURIComponent(sheetId)}/patrons`),
});

/** The ids of the person's patrons. */
export const patronIdsOf = (links: PatronLink[], personId: number) =>
  links.filter((l) => l.personId === personId).map((l) => l.patronId);

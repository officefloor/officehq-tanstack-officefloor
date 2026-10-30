import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Template = { name: string; amount: string; note: string };

export const templatesKey = (sheet: string) => ['sheet', sheet, 'templates'] as const;

/** The sheet's claim templates, by name. */
export function useTemplates(sheet: string): Template[] {
  const { data } = useQuery({
    queryKey: templatesKey(sheet),
    queryFn: () => getJson<Template[]>(`/api/sheets/${encodeURIComponent(sheet)}/templates`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

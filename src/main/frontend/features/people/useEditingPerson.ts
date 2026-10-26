import { asNumber, useSearchParam } from '../../url/useSearchParam';

/** The id of the person whose editor is open in the person form, kept in the URL. */
export function useEditingPerson() {
  return useSearchParam('editPerson', asNumber);
}

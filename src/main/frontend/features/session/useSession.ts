import { asString, useSearchParam } from '../../url/useSearchParam';
import { setSignedInUser } from '../../api/http';

/** The signed-in name (URL key `user`); '' when no one is signed in. */
export function useSignedInUser(): [string, (name: string | undefined) => void] {
  const [user, setUser] = useSearchParam('user', asString);
  setSignedInUser(user);
  return [user, setUser];
}

/** The id of the active sheet (URL key `sheet`); '' when none is loaded. */
export function useActiveSheet(): [string, (id: string | undefined) => void] {
  return useSearchParam('sheet', asString);
}

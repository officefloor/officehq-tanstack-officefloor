import { useState } from 'react';
import { useUser } from './session';

/** Sign in by typing a name. */
export function SignIn() {
  const [, setUser] = useUser();
  const [name, setName] = useState('');
  return (
    <form
      data-testid="signin"
      onSubmit={(e) => {
        e.preventDefault();
        if (name.trim() !== '') setUser(name.trim());
      }}
    >
      <label>
        Name <input data-testid="signin-name" value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}

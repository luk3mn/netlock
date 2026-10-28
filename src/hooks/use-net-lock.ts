import { useCallback, useEffect, useState } from 'react';
import NetLock, { type Status } from '../../modules/net-lock';

export function useNetLock() {
  const [status, setStatus] = useState<Status>(() => NetLock.status());
  const [now, setNow] = useState(() => Date.now());

  const refresh = useCallback(() => setStatus(NetLock.status()), []);

  useEffect(() => {
    const id = setInterval(() => {
      setNow(Date.now());
      refresh();
    }, 1000);
    return () => clearInterval(id);
  }, [refresh]);

  return { status, now, refresh };
}
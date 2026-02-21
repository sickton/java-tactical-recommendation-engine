import { useEffect } from 'react';

/** Sets document.title to "<title> | JGaffer" and restores "JGaffer" on unmount. */
export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = title ? `${title} | JGaffer` : 'JGaffer';
    return () => { document.title = 'JGaffer'; };
  }, [title]);
}

import { type ClassValue, clsx } from 'clsx';

// Note: Since we don't have tailwind-merge installed, we'll use clsx for now
// In a production environment, you might want to install tailwind-merge for better class merging
export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export default cn;
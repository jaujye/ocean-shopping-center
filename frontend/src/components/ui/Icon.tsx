import React from 'react';
import { cn } from '../../utils/cn';

// Heroicons imports - organized by category for better maintainability
import {
  // Navigation & UI
  MagnifyingGlassIcon,
  ShoppingCartIcon,
  UserIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  Bars3Icon,
  XMarkIcon,
  EllipsisVerticalIcon,
  EllipsisHorizontalIcon,
  
  // Actions
  PencilIcon,
  TrashIcon,
  DocumentDuplicateIcon,
  CheckIcon,
  PlusIcon,
  MinusIcon,
  
  // Visibility & Status  
  EyeIcon,
  EyeSlashIcon,
  
  // Filters & Views
  FunnelIcon,
  Squares2X2Icon,
  ListBulletIcon,
  
  // Communication
  BellIcon,
  ChatBubbleLeftIcon,
  EnvelopeIcon,
  
  // Security
  LockClosedIcon,
  ShieldCheckIcon,
  
  // Commerce
  CreditCardIcon,
  BanknotesIcon,
  TruckIcon,
  
  // Media
  PhotoIcon,
  VideoCameraIcon,
  MusicalNoteIcon,
  
  // Files & Documents
  DocumentTextIcon,
  FolderIcon,
  ArchiveBoxIcon,
  
  // System & Settings
  CogIcon,
  AdjustmentsHorizontalIcon,
  InformationCircleIcon,
  ExclamationTriangleIcon,
  ExclamationCircleIcon,
  CheckCircleIcon,
  
  // Arrows & Movement
  ArrowUpIcon,
  ArrowDownIcon,
  ArrowLeftIcon,
  ArrowRightIcon,
  
  // Social & Sharing
  ShareIcon,
  HeartIcon,
  StarIcon,
} from '@heroicons/react/24/outline';

import {
  // Solid variants for emphasis
  CheckCircleIcon as CheckCircleIconSolid,
  ExclamationCircleIcon as ExclamationCircleIconSolid,
  ExclamationTriangleIcon as ExclamationTriangleIconSolid,
  HeartIcon as HeartIconSolid,
  StarIcon as StarIconSolid,
  ShoppingCartIcon as ShoppingCartIconSolid,
  BellIcon as BellIconSolid,
} from '@heroicons/react/24/solid';

import {
  // Mini variants for small contexts
  CheckIcon as CheckIconMini,
  XMarkIcon as XMarkIconMini,
  PlusIcon as PlusIconMini,
  MinusIcon as MinusIconMini,
} from '@heroicons/react/20/solid';

// Icon name type for better TypeScript support
export type IconName = 
  // Navigation & UI
  | 'magnifying-glass' | 'search' 
  | 'shopping-cart'
  | 'user' | 'profile'
  | 'chevron-down' | 'chevron-up' | 'chevron-left' | 'chevron-right'
  | 'bars-3' | 'menu'
  | 'x-mark' | 'close'
  | 'ellipsis-vertical' | 'ellipsis-horizontal' | 'more'
  
  // Actions
  | 'pencil' | 'edit'
  | 'trash' | 'delete'
  | 'document-duplicate' | 'duplicate' | 'copy'
  | 'check' | 'checkmark'
  | 'plus' | 'add'
  | 'minus' | 'subtract'
  
  // Visibility & Status
  | 'eye' | 'visible'
  | 'eye-slash' | 'hidden' | 'invisible'
  
  // Filters & Views
  | 'funnel' | 'filter'
  | 'squares-2x2' | 'grid'
  | 'list-bullet' | 'list'
  
  // Communication
  | 'bell' | 'notification'
  | 'chat-bubble-left' | 'chat' | 'message'
  | 'envelope' | 'mail' | 'email'
  
  // Security
  | 'lock-closed' | 'lock' | 'secure'
  | 'shield-check' | 'shield' | 'security'
  
  // Commerce
  | 'credit-card' | 'card' | 'payment'
  | 'banknotes' | 'money' | 'cash'
  | 'truck' | 'shipping' | 'delivery'
  
  // Media
  | 'photo' | 'image'
  | 'video-camera' | 'video'
  | 'musical-note' | 'music'
  
  // Files & Documents
  | 'document-text' | 'document'
  | 'folder'
  | 'archive-box' | 'archive'
  
  // System & Settings
  | 'cog' | 'settings'
  | 'adjustments-horizontal' | 'adjustments'
  | 'information-circle' | 'info'
  | 'exclamation-triangle' | 'warning'
  | 'exclamation-circle' | 'error' | 'alert'
  | 'check-circle' | 'success'
  
  // Arrows & Movement
  | 'arrow-up' | 'arrow-down' | 'arrow-left' | 'arrow-right'
  
  // Social & Sharing
  | 'share'
  | 'heart' | 'favorite' | 'like'
  | 'star' | 'rating';

// Icon variant type
export type IconVariant = 'outline' | 'solid' | 'mini';

// Icon size type with responsive options
export type IconSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';

// Icon color options aligned with theme
export type IconColor = 
  | 'current' | 'inherit'
  | 'gray' | 'slate' | 'neutral'
  | 'red' | 'orange' | 'amber' | 'yellow'
  | 'lime' | 'green' | 'emerald' | 'teal' | 'cyan'
  | 'sky' | 'blue' | 'indigo' | 'violet' | 'purple'
  | 'fuchsia' | 'pink' | 'rose'
  | 'ocean' | 'primary'
  | 'white' | 'black';

// Icon component props interface
export interface IconProps {
  name: IconName;
  variant?: IconVariant;
  size?: IconSize;
  color?: IconColor;
  className?: string;
  'aria-label'?: string;
  'aria-hidden'?: boolean;
  title?: string;
  onClick?: () => void;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
}

// Icon mapping for better maintainability
const iconMap = {
  // Navigation & UI
  'magnifying-glass': { outline: MagnifyingGlassIcon, solid: MagnifyingGlassIcon, mini: MagnifyingGlassIcon },
  'search': { outline: MagnifyingGlassIcon, solid: MagnifyingGlassIcon, mini: MagnifyingGlassIcon },
  'shopping-cart': { outline: ShoppingCartIcon, solid: ShoppingCartIconSolid, mini: ShoppingCartIcon },
  'user': { outline: UserIcon, solid: UserIcon, mini: UserIcon },
  'profile': { outline: UserIcon, solid: UserIcon, mini: UserIcon },
  'chevron-down': { outline: ChevronDownIcon, solid: ChevronDownIcon, mini: ChevronDownIcon },
  'chevron-up': { outline: ChevronUpIcon, solid: ChevronUpIcon, mini: ChevronUpIcon },
  'chevron-left': { outline: ChevronLeftIcon, solid: ChevronLeftIcon, mini: ChevronLeftIcon },
  'chevron-right': { outline: ChevronRightIcon, solid: ChevronRightIcon, mini: ChevronRightIcon },
  'bars-3': { outline: Bars3Icon, solid: Bars3Icon, mini: Bars3Icon },
  'menu': { outline: Bars3Icon, solid: Bars3Icon, mini: Bars3Icon },
  'x-mark': { outline: XMarkIcon, solid: XMarkIcon, mini: XMarkIconMini },
  'close': { outline: XMarkIcon, solid: XMarkIcon, mini: XMarkIconMini },
  'ellipsis-vertical': { outline: EllipsisVerticalIcon, solid: EllipsisVerticalIcon, mini: EllipsisVerticalIcon },
  'ellipsis-horizontal': { outline: EllipsisHorizontalIcon, solid: EllipsisHorizontalIcon, mini: EllipsisHorizontalIcon },
  'more': { outline: EllipsisVerticalIcon, solid: EllipsisVerticalIcon, mini: EllipsisVerticalIcon },
  
  // Actions
  'pencil': { outline: PencilIcon, solid: PencilIcon, mini: PencilIcon },
  'edit': { outline: PencilIcon, solid: PencilIcon, mini: PencilIcon },
  'trash': { outline: TrashIcon, solid: TrashIcon, mini: TrashIcon },
  'delete': { outline: TrashIcon, solid: TrashIcon, mini: TrashIcon },
  'document-duplicate': { outline: DocumentDuplicateIcon, solid: DocumentDuplicateIcon, mini: DocumentDuplicateIcon },
  'duplicate': { outline: DocumentDuplicateIcon, solid: DocumentDuplicateIcon, mini: DocumentDuplicateIcon },
  'copy': { outline: DocumentDuplicateIcon, solid: DocumentDuplicateIcon, mini: DocumentDuplicateIcon },
  'check': { outline: CheckIcon, solid: CheckIcon, mini: CheckIconMini },
  'checkmark': { outline: CheckIcon, solid: CheckIcon, mini: CheckIconMini },
  'plus': { outline: PlusIcon, solid: PlusIcon, mini: PlusIconMini },
  'add': { outline: PlusIcon, solid: PlusIcon, mini: PlusIconMini },
  'minus': { outline: MinusIcon, solid: MinusIcon, mini: MinusIconMini },
  'subtract': { outline: MinusIcon, solid: MinusIcon, mini: MinusIconMini },
  
  // Visibility & Status
  'eye': { outline: EyeIcon, solid: EyeIcon, mini: EyeIcon },
  'visible': { outline: EyeIcon, solid: EyeIcon, mini: EyeIcon },
  'eye-slash': { outline: EyeSlashIcon, solid: EyeSlashIcon, mini: EyeSlashIcon },
  'hidden': { outline: EyeSlashIcon, solid: EyeSlashIcon, mini: EyeSlashIcon },
  'invisible': { outline: EyeSlashIcon, solid: EyeSlashIcon, mini: EyeSlashIcon },
  
  // Filters & Views
  'funnel': { outline: FunnelIcon, solid: FunnelIcon, mini: FunnelIcon },
  'filter': { outline: FunnelIcon, solid: FunnelIcon, mini: FunnelIcon },
  'squares-2x2': { outline: Squares2X2Icon, solid: Squares2X2Icon, mini: Squares2X2Icon },
  'grid': { outline: Squares2X2Icon, solid: Squares2X2Icon, mini: Squares2X2Icon },
  'list-bullet': { outline: ListBulletIcon, solid: ListBulletIcon, mini: ListBulletIcon },
  'list': { outline: ListBulletIcon, solid: ListBulletIcon, mini: ListBulletIcon },
  
  // Communication
  'bell': { outline: BellIcon, solid: BellIconSolid, mini: BellIcon },
  'notification': { outline: BellIcon, solid: BellIconSolid, mini: BellIcon },
  'chat-bubble-left': { outline: ChatBubbleLeftIcon, solid: ChatBubbleLeftIcon, mini: ChatBubbleLeftIcon },
  'chat': { outline: ChatBubbleLeftIcon, solid: ChatBubbleLeftIcon, mini: ChatBubbleLeftIcon },
  'message': { outline: ChatBubbleLeftIcon, solid: ChatBubbleLeftIcon, mini: ChatBubbleLeftIcon },
  'envelope': { outline: EnvelopeIcon, solid: EnvelopeIcon, mini: EnvelopeIcon },
  'mail': { outline: EnvelopeIcon, solid: EnvelopeIcon, mini: EnvelopeIcon },
  'email': { outline: EnvelopeIcon, solid: EnvelopeIcon, mini: EnvelopeIcon },
  
  // Security
  'lock-closed': { outline: LockClosedIcon, solid: LockClosedIcon, mini: LockClosedIcon },
  'lock': { outline: LockClosedIcon, solid: LockClosedIcon, mini: LockClosedIcon },
  'secure': { outline: LockClosedIcon, solid: LockClosedIcon, mini: LockClosedIcon },
  'shield-check': { outline: ShieldCheckIcon, solid: ShieldCheckIcon, mini: ShieldCheckIcon },
  'shield': { outline: ShieldCheckIcon, solid: ShieldCheckIcon, mini: ShieldCheckIcon },
  'security': { outline: ShieldCheckIcon, solid: ShieldCheckIcon, mini: ShieldCheckIcon },
  
  // Commerce
  'credit-card': { outline: CreditCardIcon, solid: CreditCardIcon, mini: CreditCardIcon },
  'card': { outline: CreditCardIcon, solid: CreditCardIcon, mini: CreditCardIcon },
  'payment': { outline: CreditCardIcon, solid: CreditCardIcon, mini: CreditCardIcon },
  'banknotes': { outline: BanknotesIcon, solid: BanknotesIcon, mini: BanknotesIcon },
  'money': { outline: BanknotesIcon, solid: BanknotesIcon, mini: BanknotesIcon },
  'cash': { outline: BanknotesIcon, solid: BanknotesIcon, mini: BanknotesIcon },
  'truck': { outline: TruckIcon, solid: TruckIcon, mini: TruckIcon },
  'shipping': { outline: TruckIcon, solid: TruckIcon, mini: TruckIcon },
  'delivery': { outline: TruckIcon, solid: TruckIcon, mini: TruckIcon },
  
  // Media
  'photo': { outline: PhotoIcon, solid: PhotoIcon, mini: PhotoIcon },
  'image': { outline: PhotoIcon, solid: PhotoIcon, mini: PhotoIcon },
  'video-camera': { outline: VideoCameraIcon, solid: VideoCameraIcon, mini: VideoCameraIcon },
  'video': { outline: VideoCameraIcon, solid: VideoCameraIcon, mini: VideoCameraIcon },
  'musical-note': { outline: MusicalNoteIcon, solid: MusicalNoteIcon, mini: MusicalNoteIcon },
  'music': { outline: MusicalNoteIcon, solid: MusicalNoteIcon, mini: MusicalNoteIcon },
  
  // Files & Documents
  'document-text': { outline: DocumentTextIcon, solid: DocumentTextIcon, mini: DocumentTextIcon },
  'document': { outline: DocumentTextIcon, solid: DocumentTextIcon, mini: DocumentTextIcon },
  'folder': { outline: FolderIcon, solid: FolderIcon, mini: FolderIcon },
  'archive-box': { outline: ArchiveBoxIcon, solid: ArchiveBoxIcon, mini: ArchiveBoxIcon },
  'archive': { outline: ArchiveBoxIcon, solid: ArchiveBoxIcon, mini: ArchiveBoxIcon },
  
  // System & Settings
  'cog': { outline: CogIcon, solid: CogIcon, mini: CogIcon },
  'settings': { outline: CogIcon, solid: CogIcon, mini: CogIcon },
  'adjustments-horizontal': { outline: AdjustmentsHorizontalIcon, solid: AdjustmentsHorizontalIcon, mini: AdjustmentsHorizontalIcon },
  'adjustments': { outline: AdjustmentsHorizontalIcon, solid: AdjustmentsHorizontalIcon, mini: AdjustmentsHorizontalIcon },
  'information-circle': { outline: InformationCircleIcon, solid: InformationCircleIcon, mini: InformationCircleIcon },
  'info': { outline: InformationCircleIcon, solid: InformationCircleIcon, mini: InformationCircleIcon },
  'exclamation-triangle': { outline: ExclamationTriangleIcon, solid: ExclamationTriangleIconSolid, mini: ExclamationTriangleIcon },
  'warning': { outline: ExclamationTriangleIcon, solid: ExclamationTriangleIconSolid, mini: ExclamationTriangleIcon },
  'exclamation-circle': { outline: ExclamationCircleIcon, solid: ExclamationCircleIconSolid, mini: ExclamationCircleIcon },
  'error': { outline: ExclamationCircleIcon, solid: ExclamationCircleIconSolid, mini: ExclamationCircleIcon },
  'alert': { outline: ExclamationCircleIcon, solid: ExclamationCircleIconSolid, mini: ExclamationCircleIcon },
  'check-circle': { outline: CheckCircleIcon, solid: CheckCircleIconSolid, mini: CheckCircleIcon },
  'success': { outline: CheckCircleIcon, solid: CheckCircleIconSolid, mini: CheckCircleIcon },
  
  // Arrows & Movement
  'arrow-up': { outline: ArrowUpIcon, solid: ArrowUpIcon, mini: ArrowUpIcon },
  'arrow-down': { outline: ArrowDownIcon, solid: ArrowDownIcon, mini: ArrowDownIcon },
  'arrow-left': { outline: ArrowLeftIcon, solid: ArrowLeftIcon, mini: ArrowLeftIcon },
  'arrow-right': { outline: ArrowRightIcon, solid: ArrowRightIcon, mini: ArrowRightIcon },
  
  // Social & Sharing
  'share': { outline: ShareIcon, solid: ShareIcon, mini: ShareIcon },
  'heart': { outline: HeartIcon, solid: HeartIconSolid, mini: HeartIcon },
  'favorite': { outline: HeartIcon, solid: HeartIconSolid, mini: HeartIcon },
  'like': { outline: HeartIcon, solid: HeartIconSolid, mini: HeartIcon },
  'star': { outline: StarIcon, solid: StarIconSolid, mini: StarIcon },
  'rating': { outline: StarIcon, solid: StarIconSolid, mini: StarIcon },
} as const;

// Size mapping with responsive considerations
const sizeMap: Record<IconSize, string> = {
  'xs': 'h-3 w-3',
  'sm': 'h-4 w-4',
  'md': 'h-5 w-5',
  'lg': 'h-6 w-6',
  'xl': 'h-8 w-8',
  '2xl': 'h-10 w-10',
  '3xl': 'h-12 w-12',
};

// Color mapping with theme support
const colorMap: Record<IconColor, string> = {
  'current': 'text-current',
  'inherit': 'text-inherit',
  'gray': 'text-gray-500 dark:text-gray-400',
  'slate': 'text-slate-500 dark:text-slate-400',
  'neutral': 'text-neutral-500 dark:text-neutral-400',
  'red': 'text-red-500 dark:text-red-400',
  'orange': 'text-orange-500 dark:text-orange-400',
  'amber': 'text-amber-500 dark:text-amber-400',
  'yellow': 'text-yellow-500 dark:text-yellow-400',
  'lime': 'text-lime-500 dark:text-lime-400',
  'green': 'text-green-500 dark:text-green-400',
  'emerald': 'text-emerald-500 dark:text-emerald-400',
  'teal': 'text-teal-500 dark:text-teal-400',
  'cyan': 'text-cyan-500 dark:text-cyan-400',
  'sky': 'text-sky-500 dark:text-sky-400',
  'blue': 'text-blue-500 dark:text-blue-400',
  'indigo': 'text-indigo-500 dark:text-indigo-400',
  'violet': 'text-violet-500 dark:text-violet-400',
  'purple': 'text-purple-500 dark:text-purple-400',
  'fuchsia': 'text-fuchsia-500 dark:text-fuchsia-400',
  'pink': 'text-pink-500 dark:text-pink-400',
  'rose': 'text-rose-500 dark:text-rose-400',
  'ocean': 'text-ocean-500 dark:text-ocean-400',
  'primary': 'text-ocean-500 dark:text-ocean-400',
  'white': 'text-white',
  'black': 'text-black dark:text-white',
};

/**
 * Unified Icon component for consistent Heroicons usage across the application.
 * 
 * Features:
 * - Consistent sizing system with responsive options
 * - Theme-aware color support (dark/light mode)
 * - Comprehensive accessibility attributes
 * - Performance optimized with tree-shaking
 * - TypeScript support with comprehensive icon name mapping
 * - Alias support for intuitive naming
 * 
 * @example
 * ```tsx
 * // Basic usage
 * <Icon name="search" />
 * 
 * // With customization
 * <Icon 
 *   name="shopping-cart" 
 *   variant="solid" 
 *   size="lg" 
 *   color="ocean" 
 *   aria-label="Shopping cart"
 * />
 * 
 * // Interactive icon
 * <Icon 
 *   name="edit" 
 *   size="sm" 
 *   onClick={handleEdit}
 *   aria-label="Edit item"
 * />
 * ```
 */
const Icon: React.FC<IconProps> = ({
  name,
  variant = 'outline',
  size = 'md',
  color = 'current',
  className,
  'aria-label': ariaLabel,
  'aria-hidden': ariaHidden,
  title,
  onClick,
  onMouseEnter,
  onMouseLeave,
  ...props
}) => {
  // Get the appropriate icon component
  const iconConfig = iconMap[name];
  if (!iconConfig) {
    console.warn(`Icon "${name}" not found in iconMap. Available icons:`, Object.keys(iconMap));
    return null;
  }
  
  const IconComponent = iconConfig[variant];
  
  // Build className
  const iconClasses = cn(
    sizeMap[size],
    colorMap[color],
    onClick && 'cursor-pointer hover:opacity-75 transition-opacity duration-200',
    'flex-shrink-0', // Prevent icon from shrinking in flex layouts
    className
  );
  
  // Accessibility: if interactive, ensure proper aria-label
  const accessibilityProps = {
    'aria-label': ariaLabel || (onClick ? `${name} action` : undefined),
    'aria-hidden': ariaHidden || (!ariaLabel && !onClick) ? true : undefined,
    title: title || (onClick && !ariaLabel ? `${name} action` : undefined),
    role: onClick ? 'button' : undefined,
    tabIndex: onClick ? 0 : undefined,
    onKeyDown: onClick ? (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        onClick();
      }
    } : undefined,
  };
  
  return (
    <IconComponent
      className={iconClasses}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      {...accessibilityProps}
      {...props}
    />
  );
};

export default Icon;

// Additional utility exports
export { iconMap, sizeMap, colorMap };
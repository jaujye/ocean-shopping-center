// Shipping and logistics types for tracking and management

export interface Shipment {
  id: string;
  orderId: string;
  trackingNumber: string;
  carrier: ShippingCarrier;
  service: ShippingService;
  status: ShipmentStatus;
  origin: ShippingAddress;
  destination: ShippingAddress;
  packageInfo: PackageInfo;
  estimatedDelivery: string;
  actualDelivery?: string;
  createdAt: string;
  updatedAt: string;
}

export type ShippingCarrier = 'dhl' | 'fedex' | 'ups' | 'usps' | 'local' | 'other';

export interface ShippingService {
  carrierId: ShippingCarrier;
  serviceCode: string;
  serviceName: string;
  deliveryTime: string; // e.g., "1-2 business days"
  features: string[]; // e.g., ["tracking", "insurance", "signature"]
}

export type ShipmentStatus = 
  | 'label_created'     // Shipping label created
  | 'picked_up'         // Package picked up by carrier
  | 'in_transit'        // In transit to destination
  | 'out_for_delivery'  // Out for delivery
  | 'delivered'         // Successfully delivered
  | 'attempted'         // Delivery attempted
  | 'delayed'           // Delivery delayed
  | 'exception'         // Exception occurred
  | 'returned'          // Returned to sender
  | 'cancelled';        // Shipment cancelled

export interface TrackingEvent {
  id: string;
  shipmentId: string;
  status: ShipmentStatus;
  description: string;
  location?: string;
  timestamp: string;
  carrierEvent?: CarrierEvent; // Raw carrier data
  isEstimated: boolean;
}

export interface CarrierEvent {
  eventCode: string;
  eventDescription: string;
  location: string;
  timestamp: string;
  rawData: any; // Original carrier response
}

export interface PackageInfo {
  weight: number;
  weightUnit: 'kg' | 'lb';
  dimensions: {
    length: number;
    width: number;
    height: number;
    unit: 'cm' | 'in';
  };
  value: number;
  currency: string;
  contents: string;
  isInsured: boolean;
  insuranceValue?: number;
  signatureRequired: boolean;
}

export interface ShippingAddress {
  name: string;
  company?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone?: string;
  email?: string;
}

export interface ShippingRate {
  carrierId: ShippingCarrier;
  serviceCode: string;
  serviceName: string;
  cost: number;
  currency: string;
  deliveryTime: string;
  estimatedDelivery: string;
  features: string[];
  isAvailable: boolean;
  restrictions?: string[];
}

export interface ShippingRateRequest {
  origin: ShippingAddress;
  destination: ShippingAddress;
  packages: PackageInfo[];
  services?: string[]; // Specific services to quote
  includeInsurance?: boolean;
  signatureRequired?: boolean;
}

export interface DeliveryAttempt {
  id: string;
  shipmentId: string;
  attemptNumber: number;
  attemptedAt: string;
  status: 'failed' | 'successful';
  reason?: string;
  nextAttemptDate?: string;
  deliveryInstructions?: string;
  recipientNotified: boolean;
}

// WebSocket message types for shipping
export interface ShippingWebSocketMessage {
  type: 'tracking_update' | 'delivery_status' | 'exception_alert' | 'rate_update';
  shipmentId?: string;
  orderId?: string;
  data: TrackingEvent | Shipment | ShippingRate | any;
  timestamp: string;
}

// Shipping preferences for users
export interface ShippingPreferences {
  userId: string;
  defaultAddress: ShippingAddress;
  preferredCarriers: ShippingCarrier[];
  deliveryInstructions: string;
  notifications: {
    trackingUpdates: boolean;
    deliveryAlerts: boolean;
    exceptionNotices: boolean;
    emailUpdates: boolean;
    smsUpdates: boolean;
  };
  signaturePreference: 'required' | 'not_required' | 'carrier_default';
  updatedAt: string;
}

// Shipping contexts and state
export interface ShippingContextType {
  shipments: Record<string, Shipment>;
  trackingEvents: Record<string, TrackingEvent[]>;
  rates: Record<string, ShippingRate[]>;
  preferences: ShippingPreferences;
  
  // Actions
  trackShipment: (trackingNumber: string, carrier?: ShippingCarrier) => Promise<Shipment>;
  getShippingRates: (request: ShippingRateRequest) => Promise<ShippingRate[]>;
  updateDeliveryInstructions: (shipmentId: string, instructions: string) => Promise<void>;
  rescheduleDelivery: (shipmentId: string, newDate: string) => Promise<void>;
  reportIssue: (shipmentId: string, issue: string, description: string) => Promise<void>;
}

// Shipping timeline display
export interface ShippingTimelineItem {
  id: string;
  status: ShipmentStatus;
  title: string;
  description: string;
  timestamp: string;
  location?: string;
  isCompleted: boolean;
  isCurrent: boolean;
  isEstimated: boolean;
  icon: string;
}

// Carrier integration interfaces
export interface CarrierAPI {
  carrierId: ShippingCarrier;
  track: (trackingNumber: string) => Promise<TrackingEvent[]>;
  getRates: (request: ShippingRateRequest) => Promise<ShippingRate[]>;
  createShipment: (shipmentData: any) => Promise<Shipment>;
  cancelShipment: (shipmentId: string) => Promise<boolean>;
}

// Mock data types for development
export interface MockShippingData {
  shipments: Shipment[];
  trackingEvents: Record<string, TrackingEvent[]>;
  rates: ShippingRate[];
  carriers: {
    id: ShippingCarrier;
    name: string;
    logo: string;
    services: ShippingService[];
  }[];
  preferences: ShippingPreferences;
}
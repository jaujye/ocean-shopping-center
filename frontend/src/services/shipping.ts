import { 
  Shipment, 
  TrackingEvent, 
  ShippingRate, 
  ShippingRateRequest, 
  ShippingCarrier, 
  ShipmentStatus, 
  DeliveryAttempt,
  MockShippingData,
  ShippingPreferences,
  ShippingTimelineItem,
  CarrierEvent
} from '../types/shipping';
import { ApiResponse } from '../types';

// Mock data for development
const MOCK_CARRIERS = [
  { 
    id: 'dhl' as ShippingCarrier, 
    name: 'DHL Express', 
    logo: '/images/carriers/dhl.png',
    services: [
      { carrierId: 'dhl' as ShippingCarrier, serviceCode: 'EXPRESS', serviceName: 'DHL Express Worldwide', deliveryTime: '1-3 business days', features: ['tracking', 'insurance', 'signature'] }
    ]
  },
  { 
    id: 'fedex' as ShippingCarrier, 
    name: 'FedEx', 
    logo: '/images/carriers/fedex.png',
    services: [
      { carrierId: 'fedex' as ShippingCarrier, serviceCode: 'OVERNIGHT', serviceName: 'FedEx Overnight', deliveryTime: '1 business day', features: ['tracking', 'insurance'] }
    ]
  },
  { 
    id: 'ups' as ShippingCarrier, 
    name: 'UPS', 
    logo: '/images/carriers/ups.png',
    services: [
      { carrierId: 'ups' as ShippingCarrier, serviceCode: 'GROUND', serviceName: 'UPS Ground', deliveryTime: '1-5 business days', features: ['tracking'] }
    ]
  },
  { 
    id: 'usps' as ShippingCarrier, 
    name: 'USPS', 
    logo: '/images/carriers/usps.png',
    services: [
      { carrierId: 'usps' as ShippingCarrier, serviceCode: 'PRIORITY', serviceName: 'Priority Mail', deliveryTime: '1-3 business days', features: ['tracking'] }
    ]
  },
  { 
    id: 'local' as ShippingCarrier, 
    name: 'Local Delivery', 
    logo: '/images/carriers/local.png',
    services: [
      { carrierId: 'local' as ShippingCarrier, serviceCode: 'SAME_DAY', serviceName: 'Same Day Delivery', deliveryTime: 'Same day', features: ['tracking', 'signature'] }
    ]
  },
];

const MOCK_SHIPMENTS: Shipment[] = [
  {
    id: 'ship-1',
    orderId: 'order-123',
    trackingNumber: 'DHL1234567890',
    carrier: 'dhl',
    service: {
      carrierId: 'dhl',
      serviceCode: 'EXPRESS',
      serviceName: 'DHL Express Worldwide',
      deliveryTime: '1-3 business days',
      features: ['tracking', 'insurance', 'signature']
    },
    status: 'in_transit',
    origin: {
      name: 'Ocean Shopping Center Warehouse',
      addressLine1: '123 Warehouse St',
      city: 'Los Angeles',
      state: 'CA',
      postalCode: '90210',
      country: 'USA',
      phone: '+1-555-0123',
    },
    destination: {
      name: 'John Doe',
      addressLine1: '456 Main St',
      addressLine2: 'Apt 2B',
      city: 'New York',
      state: 'NY',
      postalCode: '10001',
      country: 'USA',
      phone: '+1-555-0456',
    },
    packageInfo: {
      weight: 2.5,
      weightUnit: 'kg',
      dimensions: {
        length: 30,
        width: 20,
        height: 10,
        unit: 'cm'
      },
      value: 299.99,
      currency: 'USD',
      contents: 'Electronics - Wireless Headphones',
      isInsured: true,
      insuranceValue: 299.99,
      signatureRequired: true
    },
    estimatedDelivery: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'ship-2',
    orderId: 'order-124',
    trackingNumber: 'FEDEX9876543210',
    carrier: 'fedex',
    service: {
      carrierId: 'fedex',
      serviceCode: 'GROUND',
      serviceName: 'FedEx Ground',
      deliveryTime: '2-5 business days',
      features: ['tracking', 'insurance']
    },
    status: 'delivered',
    origin: {
      name: 'Ocean Shopping Center Warehouse',
      addressLine1: '123 Warehouse St',
      city: 'Los Angeles',
      state: 'CA',
      postalCode: '90210',
      country: 'USA',
      phone: '+1-555-0123',
    },
    destination: {
      name: 'Jane Smith',
      addressLine1: '789 Oak Ave',
      city: 'San Francisco',
      state: 'CA',
      postalCode: '94102',
      country: 'USA',
      phone: '+1-555-0789',
    },
    packageInfo: {
      weight: 1.2,
      weightUnit: 'kg',
      dimensions: {
        length: 25,
        width: 15,
        height: 8,
        unit: 'cm'
      },
      value: 89.99,
      currency: 'USD',
      contents: 'Clothing - T-shirt',
      isInsured: false,
      signatureRequired: false
    },
    estimatedDelivery: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
    actualDelivery: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
  },
];

const MOCK_TRACKING_EVENTS: Record<string, TrackingEvent[]> = {
  'ship-1': [
    {
      id: 'event-1',
      shipmentId: 'ship-1',
      status: 'label_created',
      description: 'Shipping label created',
      location: 'Los Angeles, CA',
      timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: 'LABEL_CREATED',
        eventDescription: 'Electronic shipping info received',
        location: 'Los Angeles, CA, US',
        timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
        rawData: {}
      }
    },
    {
      id: 'event-2',
      shipmentId: 'ship-1',
      status: 'picked_up',
      description: 'Package picked up by DHL',
      location: 'Los Angeles, CA',
      timestamp: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: 'PICKED_UP',
        eventDescription: 'Picked up by DHL',
        location: 'Los Angeles, CA, US',
        timestamp: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
        rawData: {}
      }
    },
    {
      id: 'event-3',
      shipmentId: 'ship-1',
      status: 'in_transit',
      description: 'Package in transit to destination facility',
      location: 'Phoenix, AZ',
      timestamp: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: 'IN_TRANSIT',
        eventDescription: 'Departed facility in Los Angeles, CA',
        location: 'Phoenix, AZ, US',
        timestamp: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
        rawData: {}
      }
    },
    {
      id: 'event-4',
      shipmentId: 'ship-1',
      status: 'in_transit',
      description: 'Package arrived at destination facility',
      location: 'New York, NY',
      timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: 'ARRIVED_FACILITY',
        eventDescription: 'Arrived at facility in New York, NY',
        location: 'New York, NY, US',
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        rawData: {}
      }
    },
    {
      id: 'event-5',
      shipmentId: 'ship-1',
      status: 'out_for_delivery',
      description: 'Out for delivery',
      location: 'New York, NY',
      timestamp: new Date().toISOString(),
      isEstimated: true,
      carrierEvent: {
        eventCode: 'OUT_FOR_DELIVERY',
        eventDescription: 'Out for delivery',
        location: 'New York, NY, US',
        timestamp: new Date().toISOString(),
        rawData: {}
      }
    },
  ],
  'ship-2': [
    {
      id: 'event-6',
      shipmentId: 'ship-2',
      status: 'delivered',
      description: 'Package delivered to recipient',
      location: 'San Francisco, CA',
      timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: 'DELIVERED',
        eventDescription: 'Delivered - Left at front door. Signature service not requested',
        location: 'San Francisco, CA, US',
        timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
        rawData: {}
      }
    },
  ],
};

const MOCK_SHIPPING_RATES: ShippingRate[] = [
  {
    carrierId: 'dhl',
    serviceCode: 'EXPRESS',
    serviceName: 'DHL Express Worldwide',
    cost: 25.99,
    currency: 'USD',
    deliveryTime: '1-3 business days',
    estimatedDelivery: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    features: ['tracking', 'insurance', 'signature'],
    isAvailable: true,
  },
  {
    carrierId: 'fedex',
    serviceCode: 'OVERNIGHT',
    serviceName: 'FedEx Standard Overnight',
    cost: 35.99,
    currency: 'USD',
    deliveryTime: 'Next business day',
    estimatedDelivery: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    features: ['tracking', 'insurance', 'signature'],
    isAvailable: true,
  },
  {
    carrierId: 'ups',
    serviceCode: 'GROUND',
    serviceName: 'UPS Ground',
    cost: 12.99,
    currency: 'USD',
    deliveryTime: '3-5 business days',
    estimatedDelivery: new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString(),
    features: ['tracking'],
    isAvailable: true,
  },
  {
    carrierId: 'usps',
    serviceCode: 'PRIORITY',
    serviceName: 'USPS Priority Mail',
    cost: 8.99,
    currency: 'USD',
    deliveryTime: '2-3 business days',
    estimatedDelivery: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
    features: ['tracking'],
    isAvailable: true,
  },
];

const MOCK_PREFERENCES: ShippingPreferences = {
  userId: 'user-1',
  defaultAddress: {
    name: 'John Doe',
    addressLine1: '456 Main St',
    addressLine2: 'Apt 2B',
    city: 'New York',
    state: 'NY',
    postalCode: '10001',
    country: 'USA',
    phone: '+1-555-0456',
  },
  preferredCarriers: ['dhl', 'fedex'],
  deliveryInstructions: 'Leave package at front door if no answer',
  notifications: {
    trackingUpdates: true,
    deliveryAlerts: true,
    exceptionNotices: true,
    emailUpdates: true,
    smsUpdates: false,
  },
  signaturePreference: 'not_required',
  updatedAt: new Date().toISOString(),
};

class ShippingService {
  private baseUrl: string;
  private useMockData: boolean;

  constructor(baseUrl: string = '/api', useMockData: boolean = true) {
    this.baseUrl = baseUrl;
    this.useMockData = useMockData;
  }

  /**
   * Track a shipment by tracking number
   */
  async trackShipment(trackingNumber: string, carrier?: ShippingCarrier): Promise<ApiResponse<Shipment>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 800));
      
      const shipment = MOCK_SHIPMENTS.find(s => s.trackingNumber === trackingNumber);
      if (!shipment) {
        return {
          success: false,
          data: null as any,
          message: 'Shipment not found',
          errors: ['No shipment found with this tracking number']
        };
      }

      return {
        success: true,
        data: shipment,
      };
    }

    // Real API implementation
    const params = new URLSearchParams({ trackingNumber });
    if (carrier) {
      params.append('carrier', carrier);
    }

    const response = await fetch(`${this.baseUrl}/shipping/track?${params}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to track shipment');
    }

    return response.json();
  }

  /**
   * Get tracking events for a shipment
   */
  async getTrackingEvents(shipmentId: string): Promise<ApiResponse<TrackingEvent[]>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 400));
      
      const events = MOCK_TRACKING_EVENTS[shipmentId] || [];
      return {
        success: true,
        data: events.sort((a, b) => 
          new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        ),
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/tracking/${shipmentId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch tracking events');
    }

    return response.json();
  }

  /**
   * Get shipping rates for a request
   */
  async getShippingRates(request: ShippingRateRequest): Promise<ApiResponse<ShippingRate[]>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1200));
      
      let rates = [...MOCK_SHIPPING_RATES];
      
      // Filter by requested services if specified
      if (request.services && request.services.length > 0) {
        rates = rates.filter(rate => request.services!.includes(rate.serviceCode));
      }

      // Add insurance cost if requested
      if (request.includeInsurance) {
        rates = rates.map(rate => ({
          ...rate,
          cost: rate.cost + 5.99, // Add insurance cost
          features: [...rate.features, 'insurance']
        }));
      }

      // Add signature requirement cost if requested
      if (request.signatureRequired) {
        rates = rates.map(rate => ({
          ...rate,
          cost: rate.cost + 2.99, // Add signature cost
          features: [...rate.features, 'signature']
        }));
      }

      return {
        success: true,
        data: rates,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/calculate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new Error('Failed to calculate shipping rates');
    }

    return response.json();
  }

  /**
   * Get user's shipping preferences
   */
  async getShippingPreferences(userId: string): Promise<ApiResponse<ShippingPreferences>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 300));
      
      return {
        success: true,
        data: MOCK_PREFERENCES,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/preferences/${userId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch shipping preferences');
    }

    return response.json();
  }

  /**
   * Update delivery instructions
   */
  async updateDeliveryInstructions(shipmentId: string, instructions: string): Promise<ApiResponse<void>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 400));
      
      const shipment = MOCK_SHIPMENTS.find(s => s.id === shipmentId);
      if (!shipment) {
        return {
          success: false,
          data: null as any,
          message: 'Shipment not found',
        };
      }

      // In real implementation, this would update delivery instructions
      console.log(`Updated delivery instructions for ${shipmentId}: ${instructions}`);
      
      return {
        success: true,
        data: undefined,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/delivery-instructions`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ shipmentId, instructions }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update delivery instructions');
    }

    return response.json();
  }

  /**
   * Reschedule delivery
   */
  async rescheduleDelivery(shipmentId: string, newDate: string): Promise<ApiResponse<void>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 600));
      
      const shipment = MOCK_SHIPMENTS.find(s => s.id === shipmentId);
      if (!shipment) {
        return {
          success: false,
          data: null as any,
          message: 'Shipment not found',
        };
      }

      // In real implementation, this would reschedule delivery
      console.log(`Rescheduled delivery for ${shipmentId} to ${newDate}`);
      
      return {
        success: true,
        data: undefined,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/reschedule`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ shipmentId, newDate }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to reschedule delivery');
    }

    return response.json();
  }

  /**
   * Report shipping issue
   */
  async reportIssue(shipmentId: string, issue: string, description: string): Promise<ApiResponse<void>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 500));
      
      console.log(`Reported issue for ${shipmentId}: ${issue} - ${description}`);
      
      return {
        success: true,
        data: undefined,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/shipping/report-issue`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ shipmentId, issue, description }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to report issue');
    }

    return response.json();
  }

  /**
   * Convert tracking events to timeline items for UI display
   */
  convertToTimeline(events: TrackingEvent[]): ShippingTimelineItem[] {
    const statusIcons: Record<ShipmentStatus, string> = {
      label_created: '📋',
      picked_up: '📦',
      in_transit: '🚛',
      out_for_delivery: '🚚',
      delivered: '✅',
      attempted: '⚠️',
      delayed: '⏰',
      exception: '❌',
      returned: '↩️',
      cancelled: '🚫',
    };

    const sortedEvents = [...events].sort((a, b) => 
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    const latestEvent = sortedEvents[sortedEvents.length - 1];

    return sortedEvents.map((event, index) => ({
      id: event.id,
      status: event.status,
      title: this.getStatusTitle(event.status),
      description: event.description,
      timestamp: event.timestamp,
      location: event.location,
      isCompleted: index < sortedEvents.length - 1 || event.status === 'delivered',
      isCurrent: event.id === latestEvent?.id && event.status !== 'delivered',
      isEstimated: event.isEstimated,
      icon: statusIcons[event.status],
    }));
  }

  /**
   * Get human-readable status title
   */
  private getStatusTitle(status: ShipmentStatus): string {
    const titles: Record<ShipmentStatus, string> = {
      label_created: 'Label Created',
      picked_up: 'Picked Up',
      in_transit: 'In Transit',
      out_for_delivery: 'Out for Delivery',
      delivered: 'Delivered',
      attempted: 'Delivery Attempted',
      delayed: 'Delayed',
      exception: 'Exception',
      returned: 'Returned',
      cancelled: 'Cancelled',
    };
    return titles[status];
  }

  /**
   * Get mock data for testing
   */
  getMockData(): MockShippingData {
    return {
      shipments: MOCK_SHIPMENTS,
      trackingEvents: MOCK_TRACKING_EVENTS,
      rates: MOCK_SHIPPING_RATES,
      carriers: MOCK_CARRIERS,
      preferences: MOCK_PREFERENCES,
    };
  }

  /**
   * Set mock data mode
   */
  setMockMode(useMock: boolean): void {
    this.useMockData = useMock;
  }

  /**
   * Simulate tracking update (for testing)
   */
  simulateTrackingUpdate(shipmentId: string, status: ShipmentStatus, description: string, location?: string): TrackingEvent {
    const newEvent: TrackingEvent = {
      id: `event-${Date.now()}`,
      shipmentId,
      status,
      description,
      location: location || 'Unknown Location',
      timestamp: new Date().toISOString(),
      isEstimated: false,
      carrierEvent: {
        eventCode: status.toUpperCase(),
        eventDescription: description,
        location: location || 'Unknown Location',
        timestamp: new Date().toISOString(),
        rawData: {}
      }
    };

    // Add to mock data
    if (!MOCK_TRACKING_EVENTS[shipmentId]) {
      MOCK_TRACKING_EVENTS[shipmentId] = [];
    }
    MOCK_TRACKING_EVENTS[shipmentId].push(newEvent);

    // Update shipment status
    const shipment = MOCK_SHIPMENTS.find(s => s.id === shipmentId);
    if (shipment) {
      shipment.status = status;
      shipment.updatedAt = new Date().toISOString();
      if (status === 'delivered') {
        shipment.actualDelivery = new Date().toISOString();
      }
    }

    return newEvent;
  }
}

// Create singleton instance
const shippingService = new ShippingService();

export default shippingService;
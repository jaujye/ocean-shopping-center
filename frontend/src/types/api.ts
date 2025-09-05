/**
 * Common API types for the Ocean Shopping Center application
 */

export interface ApiResponse<T> {
  content: T;
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public details?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  path: string;
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}

export interface ValidationErrorResponse extends ErrorResponse {
  errors: ValidationError[];
}
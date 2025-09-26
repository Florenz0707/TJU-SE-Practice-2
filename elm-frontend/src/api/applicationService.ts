import request from '../utils/request';
import type { Business, BusinessApplication } from './types';

// The application state enum based on the user's clarification
export const ApplicationState = {
  UNDISPOSED: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const;

export type ApplicationState = typeof ApplicationState[keyof typeof ApplicationState];


// Interface for submitting a new business application
export interface BusinessApplicationData {
  business: Business;
  applicationExplain: string;
}

// Interface for updating an application's state
export interface BusinessApplicationUpdateData {
    applicationState: ApplicationState;
}

/**
 * Submits a new business application.
 * @param applicationData - The data for the new business application.
 * @returns The created business application.
 */
export const submitBusinessApplication = async (applicationData: BusinessApplicationData): Promise<BusinessApplication> => {
  const response = await request.post('/applications/business', applicationData);
  return response.data;
};

/**
 * Retrieves all business applications (for admins).
 * @returns A list of all business applications.
 */
export const getBusinessApplications = async (): Promise<BusinessApplication[]> => {
  const response = await request.get('/applications/business');
  return response.data;
};

/**
 * Retrieves the business applications for the current user (merchant).
 * @returns A list of the current user's business applications.
 */
export const getMyBusinessApplications = async (): Promise<BusinessApplication[]> => {
  const response = await request.get('/applications/business/my');
  return response.data;
};

/**
 * Retrieves a single business application by its ID.
 * @param id - The ID of the business application.
 * @returns The business application with the specified ID.
 */
export const getBusinessApplicationById = async (id: number): Promise<BusinessApplication> => {
  const response = await request.get(`/applications/business/${id}`);
  return response.data;
};

/**
 * Updates the state of a business application (for admins).
 * @param id - The ID of the business application to update.
 * @param updateData - The data to update the application with.
 * @returns The updated business application.
 */
export const updateBusinessApplication = async (id: number, updateData: BusinessApplicationUpdateData): Promise<BusinessApplication> => {
  const response = await request.patch(`/applications/business/${id}`, updateData);
  return response.data;
};
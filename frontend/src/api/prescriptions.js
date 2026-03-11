import api from './client';

export const getById = (id) => api.get(`/prescriptions/${id}`);
export const getByElderly = (elderlyId) => api.get(`/prescriptions/elderly/${elderlyId}`);
export const create = (prescription, createdBy) =>
  api.post(`/prescriptions?createdBy=${createdBy}`, prescription);
export const update = (id, data) => api.put(`/prescriptions/${id}`, data);
export const remove = (id) => api.delete(`/prescriptions/${id}`);
export const addMedication = (prescriptionId, medication) =>
  api.post(`/prescriptions/${prescriptionId}/medications`, medication);
export const addSchedule = (medicationId, timeOfDay, reminderMinutesBefore = 15) =>
  api.post(`/prescriptions/medications/${medicationId}/schedules`, {
    timeOfDay,
    reminderMinutesBefore,
  });

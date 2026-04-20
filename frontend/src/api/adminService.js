import axiosInstance from './axiosInstance'

const adminService = {
  // Get dashboard stats (OPTIMIZED - database COUNT queries only)
  getDashboardStats: () =>
    axiosInstance.get('/admin/stats').then(res => res.data),

  // Get users with pagination (page: 0-based, size: items per page)
  getUsers: (params = {}) =>
    axiosInstance.get('/admin/users', { params: { page: params.page || 0, size: params.size || 20, ...params } }).then(res => res.data),

  // Delete user
  deleteUser: (id) =>
    axiosInstance.delete(`/admin/users/${id}`).then(res => res.data),

  // Change user role
  changeUserRole: (id, role) =>
    axiosInstance.put(`/admin/users/${id}/role`, { role }).then(res => res.data),

  // Get projects with pagination
  getProjects: (params = {}) =>
    axiosInstance.get('/admin/projects', { params: { page: params.page || 0, size: params.size || 20, ...params } }).then(res => res.data),

  // Delete project
  deleteProject: (id) =>
    axiosInstance.delete(`/admin/projects/${id}`).then(res => res.data),

  // Get tasks with pagination
  getTasks: (params = {}) =>
    axiosInstance.get('/admin/tasks', { params: { page: params.page || 0, size: params.size || 20, ...params } }).then(res => res.data),

  // Delete task
  deleteTask: (id) =>
    axiosInstance.delete(`/admin/tasks/${id}`).then(res => res.data),
}

export default adminService

import axiosInstance from './axiosInstance'

const taskService = {
  // Get tasks assigned to current user
  getTasks: (params = {}) =>
    axiosInstance.get('/tasks', { params }).then(res => res.data),

  // Get single task by ID
  getTaskById: (id) =>
    axiosInstance.get(`/tasks/${id}`).then(res => res.data),

  // Create new task (requires projectId)
  createTask: (projectId, data) =>
    axiosInstance.post(`/projects/${projectId}/tasks`, data).then(res => res.data),

  // Update task (requires projectId)
  updateTask: (projectId, taskId, data) =>
    axiosInstance.put(`/projects/${projectId}/tasks/${taskId}`, data).then(res => res.data),

  // Delete task (admin)
  deleteTask: (projectId, taskId) =>
    axiosInstance.delete(`/projects/${projectId}/tasks/${taskId}`).then(res => res.data),

  // Get tasks by project
  getTasksByProject: (projectId, params = {}) =>
    axiosInstance.get(`/projects/${projectId}/tasks`, { params }).then(res => res.data),

  // Update task status
  updateTaskStatus: (taskId, status) =>
    axiosInstance.patch(`/tasks/${taskId}/status`, null, { params: { status } }).then(res => res.data),

  // Add comment to task
  addComment: (taskId, content) =>
    axiosInstance.post(`/tasks/${taskId}/comments`, null, { params: { content } }).then(res => res.data),

  // Get task comments
  getComments: (taskId) =>
    axiosInstance.get(`/tasks/${taskId}/comments`).then(res => res.data),

  // Reorder tasks with new positions and statuses
  reorderTasks: (projectId, reorderData) =>
    axiosInstance.patch(`/projects/${projectId}/tasks/reorder`, reorderData).then(res => res.data),
}

export default taskService

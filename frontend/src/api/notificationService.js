import axiosInstance from './axiosInstance'

const notificationService = {
  // Get all notifications for current user
  getNotifications: (params = {}) =>
    axiosInstance.get('/notifications', { params }).then(res => res.data),

  // Mark notification as read
  markAsRead: (id) =>
    axiosInstance.put(`/notifications/${id}/read`).then(res => res.data),

  // Mark all notifications as read
  markAllAsRead: () =>
    axiosInstance.put('/notifications/read-all').then(res => res.data),

  // Delete notification
  deleteNotification: (id) =>
    axiosInstance.delete(`/notifications/${id}`).then(res => res.data),

  // Get unread count
  getUnreadCount: () =>
    axiosInstance.get('/notifications/unread-count').then(res => res.data),
}

export default notificationService

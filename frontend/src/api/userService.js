import axiosInstance from './axiosInstance'

const userService = {
  // Get current user profile
  getCurrentUser: () =>
    axiosInstance.get('/users/profile/me').then(res => res.data),

  // Update current user profile (needs userId)
  updateProfile: (userId, data) =>
    axiosInstance.put(`/users/${userId}`, data).then(res => res.data),

  // Change password (needs userId)
  changePassword: (userId, oldPassword, newPassword) =>
    axiosInstance.put(`/users/${userId}/password`, { oldPassword, newPassword }).then(res => res.data),

  // Get user by ID
  getUserById: (id) =>
    axiosInstance.get(`/users/${id}`).then(res => res.data),
}

export default userService

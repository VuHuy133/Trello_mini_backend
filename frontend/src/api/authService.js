import axiosInstance from './axiosInstance'

/**
 * Đăng ký tài khoản mới
 */
export const register = (data) => axiosInstance.post('/auth/register', data)

/**
 * Đăng nhập - trả về { accessToken, refreshToken, user }
 */
export const login = async (email, password) => {
  const res = await axiosInstance.post('/auth/login', { email, password })
  const { accessToken, refreshToken, user } = res.data.data

  // Lưu token vào localStorage (stateless)
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
  localStorage.setItem('user', JSON.stringify(user))

  return user
}

/**
 * Đăng xuất - blacklist token trên server rồi xóa localStorage
 */
export const logout = async () => {
  const refreshToken = localStorage.getItem('refreshToken')
  try {
    await axiosInstance.post(`/auth/logout?refreshToken=${refreshToken}`)
  } finally {
    localStorage.clear()
  }
}

/**
 * Lấy thông tin user hiện tại từ localStorage
 */
export const getCurrentUser = () => {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user) : null
}

/**
 * Kiểm tra đã đăng nhập chưa
 */
export const isAuthenticated = () => !!localStorage.getItem('accessToken')

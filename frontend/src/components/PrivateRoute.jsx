import { Navigate, Outlet } from 'react-router-dom'
import { isAuthenticated } from '../api/authService'

/**
 * Bảo vệ các route yêu cầu đăng nhập.
 * Nếu chưa có JWT trong localStorage -> redirect về /login
 */
function PrivateRoute() {
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />
}

export default PrivateRoute

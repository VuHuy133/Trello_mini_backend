import { Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomePage from './pages/HomePage'
import OAuth2CallbackPage from './pages/OAuth2CallbackPage'
import PrivateRoute from './components/PrivateRoute'

// Project Pages
import ProjectListPage from './pages/ProjectListPage'
import ProjectFormPage from './pages/ProjectFormPage'
import ProjectDetailPage from './pages/ProjectDetailPage'

// Task Pages
import TaskListPage from './pages/TaskListPage'
import TaskFormPage from './pages/TaskFormPage'
import TaskDetailPage from './pages/TaskDetailPage'

// Admin Pages
import AdminDashboardPage from './pages/AdminDashboardPage'
import AdminUsersPage from './pages/AdminUsersPage'
import AdminProjectsPage from './pages/AdminProjectsPage'
import AdminTasksPage from './pages/AdminTasksPage'

// Profile Page
import ProfilePage from './pages/ProfilePage'

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

      {/* Protected routes */}
      <Route element={<PrivateRoute />}>
        {/* Dashboard */}
        <Route path="/" element={<HomePage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />

        {/* Projects */}
        <Route path="/projects" element={<ProjectListPage />} />
        <Route path="/projects/create" element={<ProjectFormPage />} />
        <Route path="/projects/:id" element={<ProjectDetailPage />} />
        <Route path="/projects/:id/edit" element={<ProjectFormPage />} />

        {/* Tasks */}
        <Route path="/tasks" element={<TaskListPage />} />
        <Route path="/tasks/create" element={<TaskFormPage />} />
        <Route path="/tasks/:id" element={<TaskDetailPage />} />
        <Route path="/tasks/:id/edit" element={<TaskFormPage />} />

        {/* Admin Routes */}
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/projects" element={<AdminProjectsPage />} />
        <Route path="/admin/tasks" element={<AdminTasksPage />} />

        {/* Profile */}
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App

import { useEffect, useState } from 'react'
import { useNavigate, useParams, useLocation } from 'react-router-dom'
import taskService from '../api/taskService'
import projectService from '../api/projectService'

export default function TaskFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const isEdit = location.pathname.includes('/edit')

  const searchParams = new URLSearchParams(location.search)
  const preProjectId = searchParams.get('projectId') || ''
  const preStatus = searchParams.get('status') || 'TODO'

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    projectId: preProjectId,
    priority: 'MEDIUM',
    status: preStatus,
    dueDate: '',
  })
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    loadProjects()
    if (isEdit && id) {
      loadTask()
    }
  }, [id])

  const loadProjects = async () => {
    try {
      const res = await projectService.getProjects()
      setProjects(res.data || [])
    } catch (err) {
      console.error('Failed to load projects', err)
    }
  }

  const loadTask = async () => {
    try {
      setLoading(true)
      const res = await taskService.getTaskById(id)
      setFormData({
        title: res.data?.title || '',
        description: res.data?.description || '',
        projectId: res.data?.projectId || res.data?.project?.id || '',
        priority: res.data?.priority || 'MEDIUM',
        status: res.data?.status || 'TODO',
        dueDate: res.data?.dueDate ? res.data.dueDate.split('T')[0] : '',
      })
    } catch (err) {
      setError('Không thể tải công việc')
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    try {
      setLoading(true)
      if (isEdit && id) {
        await taskService.updateTask(formData.projectId, id, formData)
        navigate(`/tasks/${id}`)
      } else {
        const created = await taskService.createTask(formData.projectId, formData)
        const newId = created?.data?.id
        navigate(newId ? `/tasks/${newId}` : (formData.projectId ? `/projects/${formData.projectId}` : '/tasks'))
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Có lỗi xảy ra')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
      <style>{`
        body {
          background: linear-gradient(135deg, #f0f9ff 0%, #e0e7ff 100%);
          font-family: 'Segoe UI', -apple-system, sans-serif;
        }
        .navbar-custom { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .form-container { max-width: 800px; margin: 30px auto; padding: 20px; }
        .form-card {
          background: white; border: 1px solid #e1e4e8;
          border-radius: 8px; padding: 30px;
        }
        .form-title { font-size: 24px; font-weight: 700; color: #333; margin-bottom: 30px; }
        .form-group { margin-bottom: 20px; }
        .form-label { font-weight: 600; color: #333; margin-bottom: 8px; display: block; }
        .form-control, .form-select, textarea {
          border: 1px solid #e1e4e8; border-radius: 6px;
          padding: 10px 12px; font-size: 14px; width: 100%;
        }
        .form-control:focus, .form-select:focus, textarea:focus {
          border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); outline: none;
        }
        textarea { resize: vertical; min-height: 120px; font-family: inherit; }
        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .form-actions {
          display: flex; gap: 10px; justify-content: flex-end;
          margin-top: 30px; padding-top: 20px; border-top: 1px solid #e1e4e8;
        }
        .btn {
          padding: 10px 20px; border-radius: 6px; font-weight: 600;
          border: none; cursor: pointer; transition: all 0.3s;
        }
        .btn-primary { background: #667eea; color: white; }
        .btn-primary:hover { background: #5568d3; color: white; }
        .btn-secondary { background: #e9ecef; color: #333; }
        .btn-secondary:hover { background: #dee2e6; color: #333; }
        .alert { padding: 12px; border-radius: 6px; margin-bottom: 20px; background: #fee2e2; color: #991b1b; }
      `}</style>

      <nav className="navbar-custom">
        <div style={{ padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button onClick={() => navigate('/tasks')} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', fontWeight: 'bold' }}>
            ← Quay lại
          </button>
        </div>
      </nav>

      <div className="form-container">
        <div className="form-card">
          <h1 className="form-title">{isEdit ? 'Chỉnh sửa công việc' : 'Tạo công việc mới'}</h1>

          {error && <div className="alert">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Tên công việc</label>
              <input
                type="text"
                className="form-control"
                name="title"
                required
                value={formData.title}
                onChange={handleChange}
                placeholder="Nhập tên công việc..."
              />
            </div>

            <div className="form-group">
              <label className="form-label">Mô tả</label>
              <textarea
                className="form-control"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Nhập mô tả công việc..."
              ></textarea>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Dự án</label>
                <select
                  className="form-select"
                  name="projectId"
                  value={formData.projectId}
                  onChange={handleChange}
                  required
                >
                  <option value="">-- Chọn dự án --</option>
                  {projects.map(p => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Độ ưu tiên</label>
                <select
                  className="form-select"
                  name="priority"
                  value={formData.priority}
                  onChange={handleChange}
                >
                  <option value="LOW">Thấp</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="HIGH">Cao</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Trạng thái</label>
                <select
                  className="form-select"
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                >
                  <option value="TODO">Chưa bắt đầu</option>
                  <option value="DOING">Đang làm</option>
                  <option value="DONE">Hoàn thành</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Hạn chót</label>
                <input
                  type="date"
                  className="form-control"
                  name="dueDate"
                  value={formData.dueDate}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => navigate('/tasks')}>
                Hủy
              </button>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Đang lưu...' : (isEdit ? 'Cập nhật công việc' : 'Tạo công việc')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  )
}

import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../api/authService'
import styles from '../styles/auth.module.css'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', address: '', password: '' })
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await register(form)
      navigate('/login?success=true', { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
      <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" />

      <div className={styles.authBody}>
        <div className={styles.authContainerLarge}>
          <div className={styles.authCard}>
            {/* Header */}
            <div className={styles.authHeader}>
              <div className={styles.brandLogo}>
                <i className="fas fa-check-square"></i>
              </div>
              <h1 className={styles.brandTitle}>TaskFlow</h1>
              <p className={styles.authSubtitle}>Create your account</p>
            </div>

            {/* Error Alert */}
            {error && (
              <div className={`${styles.alert} ${styles.alertDanger}`}>
                <div className={styles.alertIcon}>
                  <i className="fas fa-exclamation-circle"></i>
                </div>
                <div className={styles.alertMessage}>{error}</div>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit}>
              {/* Full Name */}
              <div className={styles.formGroupSmall}>
                <div className={styles.formFloating}>
                  <input
                    className={styles.formControl}
                    type="text"
                    id="username"
                    name="username"
                    placeholder="Full Name"
                    value={form.username}
                    onChange={handleChange}
                    required
                  />
                  <label htmlFor="username" className={styles.formLabel}>
                    <i className="fas fa-user me-2"></i>Full Name
                  </label>
                </div>
              </div>

              {/* Email */}
              <div className={styles.formGroupSmall}>
                <div className={styles.formFloating}>
                  <input
                    className={styles.formControl}
                    type="email"
                    id="email"
                    name="email"
                    placeholder="Email"
                    value={form.email}
                    onChange={handleChange}
                    required
                  />
                  <label htmlFor="email" className={styles.formLabel}>
                    <i className="fas fa-envelope me-2"></i>Email Address
                  </label>
                </div>
              </div>

              {/* Address */}
              <div className={styles.formGroupSmall}>
                <div className={styles.formFloating}>
                  <input
                    className={styles.formControl}
                    type="text"
                    id="address"
                    name="address"
                    placeholder="Address"
                    value={form.address}
                    onChange={handleChange}
                  />
                  <label htmlFor="address" className={styles.formLabel}>
                    <i className="fas fa-map-marker-alt me-2"></i>Address
                  </label>
                </div>
              </div>

              {/* Password */}
              <div className={styles.formGroupSmall}>
                <div className={styles.formFloating}>
                  <input
                    className={styles.formControl}
                    type="password"
                    id="password"
                    name="password"
                    placeholder="Password"
                    value={form.password}
                    onChange={handleChange}
                    required
                    minLength={6}
                  />
                  <label htmlFor="password" className={styles.formLabel}>
                    <i className="fas fa-lock me-2"></i>Password
                  </label>
                </div>
              </div>

              {/* Submit Button */}
              <button type="submit" className={styles.btnAuth} disabled={loading}>
                {loading
                  ? <><i className="fas fa-spinner fa-spin me-2"></i>Creating account...</>
                  : <><i className="fas fa-arrow-right me-2"></i>Create Account</>
                }
              </button>
            </form>

            {/* Footer */}
            <div className={styles.authFooter}>
              Already have an account? <Link to="/login">Sign in here</Link>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom'
import { Container, Navbar, Nav } from 'react-bootstrap'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { CartProvider } from './contexts/CartContext'
import { ToastContainer } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'

// ✅ НЕМЕДЛЕННАЯ ЗАГРУЗКА - критичные компоненты для первого рендера
import HomePage from './components/HomePage'
import CartIcon from './components/CartIcon'
import LoadingSpinner from './components/LoadingSpinner'
import ErrorBoundary from './components/ErrorBoundary'
import ProtectedRoute from './components/ProtectedRoute'

// ⚡ ЛЕНИВАЯ ЗАГРУЗКА - загружаются только при переходе на маршрут
const CatalogPage = lazy(() => import('./components/CatalogPage'))
const GoodsDetailPage = lazy(() => import('./components/GoodsDetailPage'))
const CustomTerrariumPage = lazy(() => import('./components/CustomTerrariumPage'))
const MasterClassPlayer = lazy(() => import('./components/MasterClassPlayer'))
const Login = lazy(() => import('./components/Login'))
const Register = lazy(() => import('./components/Register'))
const CartPage = lazy(() => import('./components/CartPage'))
const ProfilePage = lazy(() => import('./components/ProfilePage'))
// AdminPanel использует named export, поэтому деструктурируем
const AdminPanel = lazy(() => import('./components/admin/AdminPanel').then(module => ({ default: module.AdminPanel })))
const NotFound = lazy(() => import('./components/NotFound'))
const CheckoutPage = lazy(() => import('./components/CheckoutPage'))
const OrderConfirmation = lazy(() => import('./components/OrderConfirmation'))

function AppContent() {
    const navigate = useNavigate()
    const location = useLocation()
    const {isAuthenticated, logout} = useAuth()

    const handleLogout = async () => {
        await logout()
        navigate('/')
    }

    // Активный пункт навбара по текущему маршруту (детальные/плеер-страницы тоже подсвечивают свой раздел)
    const isActive = (path: string): boolean => {
        const p = location.pathname
        if (path === '/') return p === '/'
        if (path === '/catalog') return p === '/catalog' || p.startsWith('/catalog/')
        if (path === '/masterclasses') return p === '/masterclasses' || p.startsWith('/masterclass')
        return p === path || p.startsWith(path + '/')
    }

    return (
        <div className="app">
            <Navbar expand="lg" className="navbar-custom">
                <Container>
                    <Navbar.Brand as={Link} to="/" className="brand-text">
                        🌿 GreenDecor
                    </Navbar.Brand>
                    <Navbar.Toggle aria-controls="basic-navbar-nav"/>
                    <Navbar.Collapse id="basic-navbar-nav">
                        <Nav className="ms-auto">
                            <Nav.Link as={Link} to="/" active={isActive('/')}>Главная</Nav.Link>
                            <Nav.Link as={Link} to="/catalog" active={isActive('/catalog')}>Растения</Nav.Link>
                            <Nav.Link as={Link} to="/terrariums" active={isActive('/terrariums')}>Флорариумы</Nav.Link>
                            <Nav.Link as={Link} to="/masterclasses" active={isActive('/masterclasses')}>Мастер-классы</Nav.Link>
                            {/* Вертикальный разделитель */}
                            <Navbar.Text className="px-2 nav-separator" style={{color: '#ccc'}}>|</Navbar.Text>
                            {isAuthenticated ? (
                                <>
                                    <Nav.Link as={Link} to="/profile" active={isActive('/profile')}>Личный кабинет</Nav.Link>
                                    <Nav.Link as="button" type="button" onClick={handleLogout} style={{background: 'none', border: 'none'}}>Выход</Nav.Link>
                                </>
                            ) : (
                                <>
                                    <Nav.Link as={Link} to="/login" state={{ from: location.pathname }}>Вход</Nav.Link>
                                    <Nav.Link as={Link} to="/register">Регистрация</Nav.Link>
                                </>
                            )}
                            {/* Вертикальный разделитель */}
                            <Navbar.Text className="px-2 nav-separator" style={{color: '#ccc'}}>|</Navbar.Text>
                            <CartIcon />
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            {/* ErrorBoundary ловит ошибки загрузки chunks */}
            <ErrorBoundary>
                <main className="app-main">
                {/* Suspense показывает LoadingSpinner пока загружаются lazy компоненты */}
                <Suspense fallback={<LoadingSpinner text="Загрузка страницы..." />}>
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/catalog" element={<CatalogPage type="PLANT" />} />
                        <Route path="/catalog/:id" element={<GoodsDetailPage />} />
                        <Route path="/terrariums" element={<CatalogPage type="TERRARIUM" />} />
                        <Route path="/custom-terrarium" element={<CustomTerrariumPage />} />
                        <Route path="/masterclasses" element={<CatalogPage type="COURSE" />} />
                        <Route path="/masterclass/:id" element={<MasterClassPlayer />} />
                        <Route path="/cart" element={<CartPage />} />
                        <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
                        <Route path="/order-confirmation" element={<ProtectedRoute><OrderConfirmation /></ProtectedRoute>} />
                        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/admin" element={<ProtectedRoute requireAdmin><AdminPanel /></ProtectedRoute>} />
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </Suspense>
                </main>
            </ErrorBoundary>

            <footer className="footer">
                <Container>
                    <p>&copy; 2026 GreenDecor Plant Shop. Все права защищены.</p>
                    <p className="footer-tagline">Растим счастье, по одному растению за раз 🌱</p>
                </Container>
            </footer>
        </div>
    )
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <CartProvider>
                    <AppContent/>
                    <ToastContainer
                        position="bottom-right"
                        autoClose={3000}
                        hideProgressBar={false}
                        newestOnTop={true}
                        closeOnClick
                        pauseOnHover
                        theme="light"
                    />
                </CartProvider>
            </AuthProvider>
        </BrowserRouter>
    )
}

export default App

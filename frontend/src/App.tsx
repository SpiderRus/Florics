import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
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

// ⚡ ЛЕНИВАЯ ЗАГРУЗКА - загружаются только при переходе на маршрут
const GoodsCatalog = lazy(() => import('./components/GoodsCatalog'))
const GoodsDetailPage = lazy(() => import('./components/GoodsDetailPage'))
const TerrariumCatalog = lazy(() => import('./components/TerrariumCatalog'))
const CustomTerrariumPage = lazy(() => import('./components/CustomTerrariumPage'))
const MasterClassCatalog = lazy(() => import('./components/MasterClassCatalog'))
const MasterClassPlayer = lazy(() => import('./components/MasterClassPlayer'))
const Login = lazy(() => import('./components/Login'))
const Register = lazy(() => import('./components/Register'))
const CartPage = lazy(() => import('./components/CartPage'))
const ProfilePage = lazy(() => import('./components/ProfilePage'))
// AdminPanel использует named export, поэтому деструктурируем
const AdminPanel = lazy(() => import('./components/admin/AdminPanel').then(module => ({ default: module.AdminPanel })))

function AppContent() {
    const navigate = useNavigate()
    const location = useLocation()
    const {isAuthenticated, logout} = useAuth()

    const handleLogout = async () => {
        await logout()
        navigate('/')
    }

    const handleLoginClick = () => {
        navigate('/login', { state: { from: location.pathname } })
    }

    return (
        <div className="app">
            <Navbar expand="lg" className="navbar-custom">
                <Container>
                    <Navbar.Brand
                        onClick={() => navigate('/')}
                        className="brand-text"
                        style={{cursor: 'pointer'}}
                    >
                        🌿 GreenDecor
                    </Navbar.Brand>
                    <Navbar.Toggle aria-controls="basic-navbar-nav"/>
                    <Navbar.Collapse id="basic-navbar-nav">
                        <Nav className="ms-auto">
                            <Nav.Link onClick={() => navigate('/')}>Главная</Nav.Link>
                            <Nav.Link onClick={() => navigate('/catalog')}>Растения</Nav.Link>
                            <Nav.Link onClick={() => navigate('/terrariums')}>Флорариумы</Nav.Link>
                            <Nav.Link onClick={() => navigate('/masterclasses')}>Мастер-классы</Nav.Link>
                            {/* Вертикальный разделитель */}
                            <Navbar.Text className="px-2" style={{color: '#ccc'}}>|</Navbar.Text>
                            {isAuthenticated ? (
                                <>
                                    <Nav.Link onClick={() => navigate('/profile')}>Личный кабинет</Nav.Link>
                                    <Nav.Link onClick={handleLogout}>Выход</Nav.Link>
                                </>
                            ) : (
                                <>
                                    <Nav.Link onClick={handleLoginClick}>Вход</Nav.Link>
                                    <Nav.Link onClick={() => navigate('/register')}>Регистрация</Nav.Link>
                                </>
                            )}
                            {/* Вертикальный разделитель */}
                            <Navbar.Text className="px-2" style={{color: '#ccc'}}>|</Navbar.Text>
                            <CartIcon />
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            {/* ErrorBoundary ловит ошибки загрузки chunks */}
            <ErrorBoundary>
                {/* Suspense показывает LoadingSpinner пока загружаются lazy компоненты */}
                <Suspense fallback={<LoadingSpinner text="Загрузка страницы..." />}>
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/catalog" element={<GoodsCatalog />} />
                        <Route path="/catalog/:id" element={<GoodsDetailPage />} />
                        <Route path="/terrariums" element={<TerrariumCatalog />} />
                        <Route path="/custom-terrarium" element={<CustomTerrariumPage />} />
                        <Route path="/masterclasses" element={<MasterClassCatalog />} />
                        <Route path="/masterclass/:id" element={<MasterClassPlayer />} />
                        <Route path="/cart" element={<CartPage />} />
                        <Route path="/profile" element={<ProfilePage />} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/admin" element={<AdminPanel />} />
                    </Routes>
                </Suspense>
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

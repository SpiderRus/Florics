import {BrowserRouter, Routes, Route, useNavigate, useLocation} from 'react-router-dom'
import {Container, Navbar, Nav} from 'react-bootstrap'
import GoodsCatalog from './components/GoodsCatalog'
import GoodsDetailPage from './components/GoodsDetailPage'
import TerrariumCatalog from './components/TerrariumCatalog'
import CustomTerrariumPage from './components/CustomTerrariumPage'
import MasterClassCatalog from './components/MasterClassCatalog'
import MasterClassPlayer from './components/MasterClassPlayer'
import Login from './components/Login'
import Register from './components/Register'
import CartPage from './components/CartPage'
import CartIcon from './components/CartIcon'
import {AuthProvider, useAuth} from './contexts/AuthContext'
import {CartProvider} from './contexts/CartContext'
import {ToastContainer} from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'

function HomePage() {
    const navigate = useNavigate()

    return (
        <>
            <div className="hero-section">
                <Container>
                    <h1 className="hero-title">Добро пожаловать в GreenDecor</h1>
                    <p className="hero-subtitle">
                        Ваш магазин красивых растений и флорариумов
                    </p>
                    <div className="hero-description">
                        <p>Откройте для себя нашу коллекцию живых растений, флорариумов ручной работы и ботанических
                            аксессуаров.</p>
                        <p>Приносим красоту природы в ваш дом, по одному растению за раз.</p>
                    </div>
                </Container>
            </div>

            <Container className="content-section">
                <div className="feature-grid">
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/catalog')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🪴</div>
                        <h3>Комнатные растения</h3>
                        <p>Широкий выбор комнатных растений для любого пространства</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/terrariums')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🌱</div>
                        <h3>Флорариумы</h3>
                        <p>Стеклянные флорариумы ручной работы с живыми экосистемами</p>
                    </div>
                    <div
                        className="feature-card clickable-card"
                        onClick={() => navigate('/masterclasses')}
                        style={{cursor: 'pointer'}}
                    >
                        <div className="feature-icon">🎓</div>
                        <h3>Мастер-классы</h3>
                        <p>Обучающие видеокурсы по созданию флорариумов и уходу за растениями</p>
                    </div>
                </div>
            </Container>
        </>
    )
}

function AppContent() {
    const navigate = useNavigate()
    const location = useLocation()
    const {user, isAuthenticated, logout} = useAuth()

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
                                    <Nav.Link disabled>Привет, {user?.name}</Nav.Link>
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

            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/catalog" element={<GoodsCatalog/>}/>
                <Route path="/catalog/:id" element={<GoodsDetailPage/>}/>
                <Route path="/terrariums" element={<TerrariumCatalog/>}/>
                <Route path="/custom-terrarium" element={<CustomTerrariumPage/>}/>
                <Route path="/masterclasses" element={<MasterClassCatalog/>}/>
                <Route path="/masterclass/:id" element={<MasterClassPlayer/>}/>
                <Route path="/cart" element={<CartPage/>}/>
                <Route path="/login" element={<Login/>}/>
                <Route path="/register" element={<Register/>}/>
            </Routes>

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

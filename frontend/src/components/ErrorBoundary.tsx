import { Component, ReactNode } from 'react'
import { Container } from 'react-bootstrap'

interface ErrorBoundaryProps {
    children: ReactNode
}

interface ErrorBoundaryState {
    hasError: boolean
    error?: Error
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props)
        this.state = { hasError: false }
    }

    static getDerivedStateFromError(error: Error): ErrorBoundaryState {
        return { hasError: true, error }
    }

    componentDidCatch(error: Error, errorInfo: any) {
        console.error('ErrorBoundary caught an error:', error, errorInfo)
    }

    render() {
        if (this.state.hasError) {
            return (
                <Container style={{ textAlign: 'center', padding: '100px 20px' }}>
                    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
                        <h2 style={{ color: '#dc3545', marginBottom: '20px' }}>
                            ⚠️ Ошибка загрузки
                        </h2>
                        <p style={{ color: '#6c757d', marginBottom: '30px' }}>
                            Произошла ошибка при загрузке страницы.
                            Пожалуйста, проверьте подключение к интернету и попробуйте снова.
                        </p>
                        {this.state.error && (
                            <details style={{
                                textAlign: 'left',
                                background: '#f8f9fa',
                                padding: '15px',
                                borderRadius: '5px',
                                marginBottom: '20px'
                            }}>
                                <summary style={{ cursor: 'pointer', fontWeight: 'bold' }}>
                                    Технические детали
                                </summary>
                                <pre style={{
                                    marginTop: '10px',
                                    fontSize: '12px',
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-word'
                                }}>
                                    {this.state.error.message}
                                </pre>
                            </details>
                        )}
                        <button
                            className="btn btn-success"
                            onClick={() => window.location.reload()}
                        >
                            🔄 Перезагрузить страницу
                        </button>
                    </div>
                </Container>
            )
        }

        return this.props.children
    }
}

export default ErrorBoundary

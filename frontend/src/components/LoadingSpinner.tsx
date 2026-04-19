import { CSSProperties } from 'react'

interface LoadingSpinnerProps {
    text?: string
    minHeight?: string
}

export const LoadingSpinner = ({ text = 'Загрузка...', minHeight = '400px' }: LoadingSpinnerProps) => {
    const containerStyle: CSSProperties = {
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: minHeight,
        gap: '20px'
    }

    return (
        <div style={containerStyle}>
            <div className="spinner-border text-success" role="status" style={{ width: '3rem', height: '3rem' }}>
                <span className="visually-hidden">{text}</span>
            </div>
            <p className="text-muted">{text}</p>
        </div>
    )
}

export default LoadingSpinner

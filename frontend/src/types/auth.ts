export interface User {
    id: string; // UUID
    name: string;
    email: string;
    canPurchase: boolean;
    isAdmin?: boolean; // true для админов, null/undefined для обычных пользователей
}

export interface AuthRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    name: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: User;
}

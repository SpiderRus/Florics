export interface User {
    id: number;
    name: string;
    email: string;
    roles: string[];
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

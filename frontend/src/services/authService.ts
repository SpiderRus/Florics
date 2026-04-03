import axios from 'axios';
import { AuthRequest, RegisterRequest, AuthResponse, User } from '../types/auth';

const API_BASE_URL = '/api/auth';

export const authService = {
    login: async (email: string, password: string): Promise<AuthResponse> => {
        const response = await axios.post<AuthResponse>(
            `${API_BASE_URL}/login`,
            { email, password } as AuthRequest
        );
        return response.data;
    },

    register: async (email: string, name: string, password: string): Promise<AuthResponse> => {
        const response = await axios.post<AuthResponse>(
            `${API_BASE_URL}/register`,
            { email, name, password } as RegisterRequest
        );
        return response.data;
    },

    logout: async (token: string): Promise<void> => {
        await axios.post(
            `${API_BASE_URL}/logout`,
            {},
            { headers: { Authorization: `Bearer ${token}` } }
        );
    },

    getCurrentUser: async (token: string): Promise<User> => {
        const response = await axios.get<User>(
            `${API_BASE_URL}/me`,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        return response.data;
    }
};

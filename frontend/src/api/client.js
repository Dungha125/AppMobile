import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

// Thiết bị thật (iOS/Android WiFi): IP máy PC | Emulator: iOS=localhost, Android=10.0.2.2 | Web: localhost
// IP hiện tại của máy: 172.16.59.113 (lấy từ Metro Bundler - cập nhật 24/02/2026)
const API_URL = Platform.OS === 'web'
  ? 'http://localhost:8082/api'
  : Platform.OS === 'android'
    ? 'http://10.0.2.2:8082/api'  // Android emulator
    : 'http://172.16.59.113:8082/api';  // iOS device WiFi hoặc Android device WiFi

const api = axios.create({
  baseURL: API_URL,
  timeout: 15000, // 15 seconds timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// Log API URL để debug (chỉ 1 lần)
let hasLoggedUrl = false;
if (!hasLoggedUrl) {
  console.log('🌐 API_URL:', API_URL);
  hasLoggedUrl = true;
}

api.interceptors.request.use(
  async (config) => {
    console.log(`→ ${config.method?.toUpperCase()} ${config.url}`);
    const token = await AsyncStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('❌ Request error:', error.message);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    console.log(`✓ ${response.status} ${response.config.url}`);
    return response;
  },
  async (error) => {
    if (error.code === 'ECONNABORTED') {
      console.error('❌ Request timeout:', error.config?.url);
    } else if (error.response) {
      console.error(`❌ ${error.response.status} ${error.config?.url}:`, error.response.data);
    } else if (error.request) {
      console.error('❌ No response from server:', error.config?.url);
      console.error('   Check: Backend running? IP correct? Same WiFi?');
    } else {
      console.error('❌ Error:', error.message);
    }
    
    if (error.response?.status === 401) {
      await AsyncStorage.removeItem('token');
      await AsyncStorage.removeItem('user');
    }
    return Promise.reject(error);
  }
);

export default api;
export { API_URL };

import React, { createContext, useContext, useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import api from '../api/client';
import passiveCheckInService from '../services/PassiveCheckInService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUser();
  }, []);

  // Trigger PASSIVE check-in khi user thay đổi (login)
  useEffect(() => {
    if (user && user.role === 'ELDERLY') {
      console.log('👤 User logged in as ELDERLY, enabling PASSIVE check-in');
      passiveCheckInService.onAppOpen(user.id);
    }
  }, [user]);

  const loadUser = async () => {
    try {
      const token = await AsyncStorage.getItem('token');
      const userStr = await AsyncStorage.getItem('user');
      
      if (token && userStr) {
        const cachedUser = JSON.parse(userStr);
        setUser(cachedUser);
        setLoading(false); // ✅ Set loading false NGAY, không chờ API
        
        // Refresh user data từ server trong background (không block UI)
        // Chỉ refresh nếu cần, không làm mỗi lần load
        const lastRefresh = await AsyncStorage.getItem('lastUserRefresh');
        const now = Date.now();
        const ONE_HOUR = 60 * 60 * 1000;
        
        // Chỉ refresh nếu > 1 giờ từ lần cuối
        if (!lastRefresh || (now - parseInt(lastRefresh)) > ONE_HOUR) {
          setTimeout(async () => {
            try {
              console.log('🔄 Refreshing user data in background...');
              const res = await api.get('/users/me');
              if (res.data?.data) {
                const u = res.data.data;
                console.log('✓ User data refreshed from server');
                setUser(u);
                await AsyncStorage.setItem('user', JSON.stringify(u));
                await AsyncStorage.setItem('lastUserRefresh', now.toString());
              }
            } catch (e) {
              // Silent fail - không làm gì cả
              console.log('⚠️ Background refresh failed (OK to ignore)');
            }
          }, 2000); // Delay 2 giây để UI load trước
        } else {
          console.log('✓ Using cached user (refreshed recently)');
        }
      } else {
        setUser(null);
        setLoading(false);
      }
    } catch (e) {
      console.error('Error in loadUser:', e);
      setUser(null);
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    const res = await apiLogin(email, password);
    const wrap = res.data || res;
    const data = wrap.data || wrap;
    if (!data.token) throw new Error(wrap.message || data.message || 'Đăng nhập thất bại');
    await AsyncStorage.setItem('token', data.token);
    await AsyncStorage.setItem('user', JSON.stringify({
      id: data.userId,
      email: data.email,
      fullName: data.fullName,
      role: data.role,
    }));
    setUser({ id: data.userId, email: data.email, fullName: data.fullName, role: data.role });
  };

  const register = async (data) => {
    const res = await apiRegister(data);
    const wrap = res.data || res;
    const d = wrap.data || wrap;
    if (!d.token) throw new Error(wrap.message || d.message || 'Đăng ký thất bại');
    await AsyncStorage.setItem('token', d.token);
    await AsyncStorage.setItem('user', JSON.stringify({
      id: d.userId,
      email: d.email,
      fullName: d.fullName,
      role: d.role,
    }));
    setUser({ id: d.userId, email: d.email, fullName: d.fullName, role: d.role });
  };

  const logout = async () => {
    await AsyncStorage.removeItem('token');
    await AsyncStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser: loadUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};

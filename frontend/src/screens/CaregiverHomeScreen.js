import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  RefreshControl,
  Linking,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../context/AuthContext';
import { getLinkedElderly } from '../api/users';
import { getByCaregiver, getUnreadCount } from '../api/alerts';

export default function CaregiverHomeScreen({ navigation }) {
  const { user, logout } = useAuth();
  const [elderly, setElderly] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const load = async () => {
    if (!user?.id) return;
    try {
      const [elderlyRes, alertsRes, countRes] = await Promise.all([
        getLinkedElderly(user.id),
        getByCaregiver(user.id),
        getUnreadCount(user.id),
      ]);
      setElderly(elderlyRes.data?.data || []);
      setAlerts(alertsRes.data?.data || []);
      setUnreadCount(countRes.data?.data ?? 0);
    } catch (e) {
      console.warn(e);
    }
  };

  useEffect(() => {
    load();
  }, [user?.id]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [user?.id])
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  const sortedAlerts = useMemo(() => {
    const list = [...(alerts || [])];
    list.sort((a, b) => {
      if (!a.isRead && b.isRead) return -1;
      if (a.isRead && !b.isRead) return 1;
      const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return tb - ta;
    });
    return list;
  }, [alerts]);

  const openLocation = (lat, lng) => {
    if (lat != null && lng != null) {
      Linking.openURL(`https://www.google.com/maps?q=${lat},${lng}`);
    }
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      <View style={styles.header}>
        <Text style={styles.greeting} numberOfLines={1}>Xin chào, {user?.fullName || ''}</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity style={styles.chatbotBtn} onPress={() => navigation.navigate('Account')}>
            <Text style={styles.chatbotBtnText}>👤 TK</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.chatbotBtn} onPress={() => navigation.navigate('ChatList')}>
            <Text style={styles.chatbotBtnText}>💬 Chat</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.chatbotBtn} onPress={() => navigation.navigate('Chatbot')}>
            <Text style={styles.chatbotBtnText}>💊 Thuốc</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={logout} style={styles.logoutBtn}>
            <Text style={styles.logoutText}>Đăng xuất</Text>
          </TouchableOpacity>
        </View>
      </View>

      {unreadCount > 0 && (
        <TouchableOpacity
          style={styles.alertBanner}
          onPress={() => navigation.navigate('Alerts')}
        >
          <Text style={styles.alertBannerText}>
            Có {unreadCount} cảnh báo chưa đọc
          </Text>
        </TouchableOpacity>
      )}

      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>Người cao tuổi đang chăm sóc</Text>
          <TouchableOpacity onPress={() => navigation.navigate('LinkElderly')}>
            <Text style={styles.link}>Liên kết thêm</Text>
          </TouchableOpacity>
        </View>
        {elderly.length === 0 ? (
          <Text style={styles.empty}>Chưa liên kết ai. Bấm Liên kết thêm để thêm.</Text>
        ) : (
          elderly.map((e) => (
            <TouchableOpacity
              key={e.id}
              style={styles.elderlyItem}
              onPress={() => navigation.navigate('ElderlyDetail', { elderlyId: e.id, elderlyName: e.fullName })}
            >
              <Text style={styles.elderlyName}>{e.fullName}</Text>
              <Text style={styles.elderlyEmail}>{e.email}</Text>
            </TouchableOpacity>
          ))
        )}
      </View>

      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>Cảnh báo gần đây</Text>
          <TouchableOpacity onPress={() => navigation.navigate('Alerts')}>
            <Text style={styles.link}>Xem tất cả</Text>
          </TouchableOpacity>
        </View>
        {alerts.length === 0 ? (
          <Text style={styles.empty}>Không có cảnh báo</Text>
        ) : (
          sortedAlerts.slice(0, 5).map((a) => {
            const hasLocation = a.latitude != null && a.longitude != null;
            const locStr = hasLocation ? `${a.latitude}, ${a.longitude}` : null;
            return (
              <TouchableOpacity
                key={a.id}
                style={[styles.alertItem, !a.isRead && styles.alertUnread, !a.isRead && styles.alertUnreadBold]}
                onPress={() => navigation.navigate('Alerts')}
                activeOpacity={0.7}
              >
                {!a.isRead && <View style={styles.alertDot} />}
                <View style={styles.alertContent}>
                  <Text style={[styles.alertTitle, !a.isRead && styles.alertTitleUnread]}>{a.title}</Text>
                  <Text style={styles.alertMsg} numberOfLines={2}>{a.message}</Text>
                  {hasLocation && (
                    <TouchableOpacity
                      style={styles.alertLocation}
                      onPress={(e) => { e.stopPropagation(); openLocation(a.latitude, a.longitude); }}
                    >
                      <Text style={styles.alertLocationText}>📍 Vị trí: {a.elderly?.fullName || 'NCT'} — {locStr} (Bấm mở bản đồ)</Text>
                    </TouchableOpacity>
                  )}
                  <Text style={styles.alertTime}>
                    {a.createdAt ? new Date(a.createdAt).toLocaleString('vi-VN') : ''}
                  </Text>
                </View>
              </TouchableOpacity>
            );
          })
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  header: {
    padding: 16,
    backgroundColor: '#0f766e',
  },
  greeting: { fontSize: 18, color: '#fff', fontWeight: '600', marginBottom: 10 },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flexWrap: 'wrap',
  },
  chatbotBtn: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
  },
  chatbotBtnText: { color: '#fff', fontSize: 13, fontWeight: '500' },
  logoutBtn: { padding: 8 },
  logoutText: { color: '#fff', fontSize: 14 },
  alertBanner: {
    backgroundColor: '#dc2626',
    padding: 12,
    margin: 16,
    borderRadius: 8,
  },
  alertBannerText: { color: '#fff', fontWeight: '600', textAlign: 'center' },
  card: {
    backgroundColor: '#fff',
    margin: 16,
    padding: 16,
    borderRadius: 12,
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cardTitle: { fontSize: 18, fontWeight: '600', marginBottom: 12 },
  link: { color: '#0f766e', fontSize: 14 },
  empty: { color: '#999' },
  elderlyItem: {
    padding: 12,
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    marginBottom: 8,
  },
  elderlyName: { fontSize: 16, fontWeight: '600' },
  elderlyEmail: { fontSize: 12, color: '#666' },
  alertItem: { padding: 12, borderBottomWidth: 1, borderBottomColor: '#eee', flexDirection: 'row', alignItems: 'flex-start' },
  alertUnread: { backgroundColor: '#fef2f2', borderLeftWidth: 4, borderLeftColor: '#dc2626' },
  alertUnreadBold: { borderBottomColor: '#fecaca' },
  alertDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#dc2626', marginRight: 10, marginTop: 6 },
  alertContent: { flex: 1 },
  alertTitle: { fontSize: 14, fontWeight: '600' },
  alertTitleUnread: { color: '#dc2626', fontWeight: '700' },
  alertLocation: { marginTop: 6 },
  alertLocationText: { fontSize: 12, color: '#0f766e', fontWeight: '500', textDecorationLine: 'underline' },
  alertMsg: { fontSize: 12, color: '#666', marginTop: 4 },
  alertTime: { fontSize: 11, color: '#999', marginTop: 4 },
});

import React, { useState, useEffect, useMemo } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TextInput,
  Modal,
  Switch,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useAlert } from '../utils/showAlert';
import { getAdminConfig, setAdminConfig } from '../api/admin';

const CATEGORY_NAMES = {
  NOTIFICATION: '📢 Thông Báo',
  TIMING: '⏰ Thời Gian',
  FIREBASE: '🔥 Firebase',
  SECURITY: '🔒 Bảo Mật',
  LIMITS: '🎯 Giới Hạn',
  LOCATION: '📍 Vị Trí',
  API: '🌐 API',
  MAINTENANCE: '🔧 Bảo Trì',
  APPLICATION: '📱 Ứng Dụng',
  EMAIL_SMS: '📧 Email/SMS',
};

export default function AdminConfigScreen({ navigation }) {
  const { showAlert } = useAlert();
  const [list, setList] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [editingKey, setEditingKey] = useState('');
  const [editValue, setEditValue] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      const res = await getAdminConfig();
      setList(res.data?.data || []);
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không tải cấu hình', type: 'error' });
    }
  };

  useEffect(() => {
    load();
  }, []);

  useFocusEffect(
    React.useCallback(() => {
      load();
    }, [])
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  // Nhóm configs theo category
  const groupedConfigs = useMemo(() => {
    const grouped = {};
    list.forEach((item) => {
      const category = item.category || 'OTHER';
      if (!grouped[category]) {
        grouped[category] = [];
      }
      grouped[category].push(item);
    });
    return grouped;
  }, [list]);

  const openEdit = (item) => {
    setEditingItem(item);
    setEditingKey(item?.configKey || '');
    setEditValue(item?.configValue || '');
    setEditDesc(item?.description || '');
    setModalVisible(true);
  };

  const openNew = () => {
    setEditingItem(null);
    setEditingKey('');
    setEditValue('');
    setEditDesc('');
    setModalVisible(true);
  };

  const save = async () => {
    const key = (editingKey || '').trim();
    if (!key) {
      showAlert({ title: 'Lỗi', message: 'Nhập key', type: 'error' });
      return;
    }
    setSaving(true);
    try {
      await setAdminConfig({ 
        configKey: key, 
        configValue: editValue, 
        description: editDesc || null 
      });
      showAlert({ title: 'Thành công', message: 'Đã lưu cấu hình', type: 'success' });
      setModalVisible(false);
      await load();
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không lưu được', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const toggleBooleanConfig = async (item) => {
    const newValue = item.configValue === 'true' ? 'false' : 'true';
    try {
      await setAdminConfig({
        configKey: item.configKey,
        configValue: newValue,
      });
      await load();
    } catch (e) {
      showAlert({ title: 'Lỗi', message: 'Không cập nhật được', type: 'error' });
    }
  };

  const renderConfigItem = (item) => {
    const isBoolean = item.configType === 'boolean';
    const displayName = item.displayName || item.configKey;

    if (isBoolean) {
      return (
        <View key={item.id} style={styles.configRow}>
          <View style={styles.configRowLeft}>
            <Text style={styles.configDisplayName}>{displayName}</Text>
            <Text style={styles.configKey}>{item.configKey}</Text>
            {item.description ? (
              <Text style={styles.configDesc}>{item.description}</Text>
            ) : null}
          </View>
          <Switch
            value={item.configValue === 'true'}
            onValueChange={() => toggleBooleanConfig(item)}
            trackColor={{ false: '#d1d5db', true: '#0f766e' }}
            thumbColor={item.configValue === 'true' ? '#fff' : '#f4f3f4'}
          />
        </View>
      );
    }

    return (
      <TouchableOpacity
        key={item.id}
        style={styles.configRow}
        onPress={() => openEdit(item)}
      >
        <View style={styles.configRowLeft}>
          <Text style={styles.configDisplayName}>{displayName}</Text>
          <Text style={styles.configKey}>{item.configKey}</Text>
          <View style={styles.valueRow}>
            <Text style={styles.configValue}>{item.configValue || '(trống)'}</Text>
            {item.configType && item.configType !== 'string' && (
              <Text style={styles.typeBadge}>{item.configType}</Text>
            )}
          </View>
          {item.description ? (
            <Text style={styles.configDesc}>{item.description}</Text>
          ) : null}
        </View>
        <Text style={styles.editIcon}>›</Text>
      </TouchableOpacity>
    );
  };

  return (
    <>
      <ScrollView
        style={styles.container}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      >
        <TouchableOpacity style={styles.addBtn} onPress={openNew}>
          <Text style={styles.addBtnText}>+ Thêm Cấu Hình Mới</Text>
        </TouchableOpacity>

        {Object.entries(groupedConfigs).map(([category, configs]) => (
          <View key={category} style={styles.categorySection}>
            <Text style={styles.categoryTitle}>
              {CATEGORY_NAMES[category] || category}
            </Text>
            {configs.map((item) => renderConfigItem(item))}
          </View>
        ))}

        {list.length === 0 && (
          <Text style={styles.empty}>Chưa có cấu hình. Bấm "Thêm cấu hình" để tạo.</Text>
        )}
      </ScrollView>

      <Modal visible={modalVisible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>{editingKey ? 'Sửa cấu hình' : 'Thêm cấu hình'}</Text>
            <TextInput
              style={styles.input}
              placeholder="Key (vd: app_name)"
              value={editingKey}
              onChangeText={setEditingKey}
              editable={!editingKey}
              placeholderTextColor="#999"
            />
            <TextInput
              style={[styles.input, styles.inputMultiline]}
              placeholder="Value"
              value={editValue}
              onChangeText={setEditValue}
              multiline
              placeholderTextColor="#999"
            />
            <TextInput
              style={styles.input}
              placeholder="Mô tả (tùy chọn)"
              value={editDesc}
              onChangeText={setEditDesc}
              placeholderTextColor="#999"
            />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => setModalVisible(false)}>
                <Text style={styles.cancelBtnText}>Hủy</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.saveBtn} onPress={save} disabled={saving}>
                <Text style={styles.saveBtnText}>{saving ? 'Đang lưu...' : 'Lưu'}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  addBtn: {
    margin: 16,
    padding: 14,
    backgroundColor: '#0f766e',
    borderRadius: 8,
    alignItems: 'center',
  },
  addBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  categorySection: {
    marginBottom: 16,
  },
  categoryTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0f766e',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#e6f7f5',
    marginBottom: 8,
  },
  configRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginBottom: 8,
    padding: 14,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  configRowLeft: { flex: 1, marginRight: 12 },
  configDisplayName: { fontSize: 16, fontWeight: '600', color: '#111827', marginBottom: 4 },
  configKey: { fontSize: 11, color: '#6b7280', fontFamily: 'monospace' },
  valueRow: { flexDirection: 'row', alignItems: 'center', marginTop: 6, gap: 8 },
  configValue: { fontSize: 14, color: '#0f766e', fontWeight: '500' },
  typeBadge: { 
    fontSize: 10, 
    color: '#6b7280', 
    backgroundColor: '#f3f4f6', 
    paddingHorizontal: 6, 
    paddingVertical: 2, 
    borderRadius: 4,
    fontFamily: 'monospace',
  },
  configDesc: { fontSize: 12, color: '#6b7280', marginTop: 6, lineHeight: 16 },
  editIcon: { fontSize: 24, color: '#d1d5db', marginLeft: 8 },
  empty: { textAlign: 'center', color: '#999', padding: 24 },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    padding: 24,
  },
  modalBox: { backgroundColor: '#fff', borderRadius: 12, padding: 20 },
  modalTitle: { fontSize: 18, fontWeight: '600', marginBottom: 16 },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    marginBottom: 12,
  },
  inputMultiline: { minHeight: 80, textAlignVertical: 'top' },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 8 },
  cancelBtn: { padding: 12 },
  cancelBtnText: { color: '#666', fontSize: 16 },
  saveBtn: {
    backgroundColor: '#0f766e',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
  },
  saveBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});

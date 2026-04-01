import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useAlert } from '../utils/showAlert';
import { getById, update } from '../api/prescriptions';

export default function EditPrescriptionScreen({ route, navigation }) {
  const { id } = route.params || {};
  const { showAlert } = useAlert();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [doctorName, setDoctorName] = useState('');
  const [notes, setNotes] = useState('');

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await getById(id);
      const p = res.data?.data;
      setTitle(p?.title || '');
      setDoctorName(p?.doctorName || '');
      setNotes(p?.notes || '');
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không tải được đơn thuốc', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    navigation.setOptions({ title: 'Sửa đơn thuốc' });
    load();
  }, [id]);

  const save = async () => {
    if (!title.trim()) {
      showAlert({ title: 'Lỗi', message: 'Nhập tiêu đề đơn thuốc', type: 'error' });
      return;
    }
    setSaving(true);
    try {
      await update(id, { title: title.trim(), doctorName: doctorName.trim() || null, notes: notes.trim() || null });
      showAlert({ title: 'Thành công', message: 'Đã cập nhật đơn thuốc', type: 'success' });
      navigation.goBack();
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không lưu được', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <Text style={styles.empty}>Đang tải...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text style={styles.label}>Tiêu đề</Text>
      <TextInput style={styles.input} value={title} onChangeText={setTitle} placeholder="vd: Đơn thuốc tháng 3" />

      <Text style={styles.label}>Bác sĩ</Text>
      <TextInput style={styles.input} value={doctorName} onChangeText={setDoctorName} placeholder="vd: BS Nguyễn Văn A" />

      <Text style={styles.label}>Ghi chú</Text>
      <TextInput style={[styles.input, styles.multi]} value={notes} onChangeText={setNotes} placeholder="..." multiline />

      <TouchableOpacity style={[styles.btn, saving && styles.btnDisabled]} onPress={save} disabled={saving}>
        <Text style={styles.btnText}>{saving ? 'Đang lưu...' : 'Lưu'}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { padding: 16, paddingBottom: 28 },
  label: { fontSize: 12, color: '#6b7280', fontWeight: '800', marginTop: 12, marginBottom: 6 },
  input: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, fontSize: 15 },
  multi: { minHeight: 100, textAlignVertical: 'top' },
  btn: { marginTop: 16, backgroundColor: '#0f766e', padding: 14, borderRadius: 12, alignItems: 'center' },
  btnText: { color: '#fff', fontWeight: '900', fontSize: 16 },
  btnDisabled: { opacity: 0.7 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f5f5f5' },
  empty: { color: '#9ca3af' },
});


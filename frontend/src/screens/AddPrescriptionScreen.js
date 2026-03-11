import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { useAlert } from '../utils/showAlert';
import { create, addMedication, addSchedule } from '../api/prescriptions';

function toDateStr(d) {
  if (!d) return '';
  const x = d instanceof Date ? d : new Date(d);
  const pad = (n) => String(n).padStart(2, '0');
  return `${x.getFullYear()}-${pad(x.getMonth() + 1)}-${pad(x.getDate())}`;
}

export default function AddPrescriptionScreen({ route, navigation }) {
  const { elderlyId, elderlyName } = route.params || {};
  const { user } = useAuth();
  const { showAlert } = useAlert();
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [doctorName, setDoctorName] = useState('');
  const [notes, setNotes] = useState('');
  const [startDate, setStartDate] = useState(toDateStr(new Date()));
  const [endDate, setEndDate] = useState('');
  const [medName, setMedName] = useState('');
  const [medDosage, setMedDosage] = useState('');
  const [medTime, setMedTime] = useState('08:00');

  const handleSave = async () => {
    const t = title?.trim();
    if (!t) {
      showAlert({ title: 'Lỗi', message: 'Vui lòng nhập tên đơn thuốc', type: 'error' });
      return;
    }
    if (!elderlyId || !user?.id) return;
    setSaving(true);
    try {
      const presRes = await create(
        {
          elderly: { id: elderlyId },
          title: t,
          doctorName: doctorName.trim() || null,
          notes: notes.trim() || null,
          startDate: startDate || null,
          endDate: endDate || null,
        },
        user.id
      );
      const prescription = presRes.data?.data;
      if (!prescription?.id) throw new Error('Không tạo được đơn');

      const m = medName?.trim();
      if (m) {
        const medRes = await addMedication(prescription.id, {
          name: m,
          dosage: medDosage.trim() || null,
          instructions: null,
          quantity: 1,
        });
        const medication = medRes.data?.data;
        if (medication?.id) {
          const timeStr = medTime || '08:00';
          await addSchedule(medication.id, timeStr, 15);
        }
      }

      showAlert({
        title: 'Thành công',
        message: 'Đã thêm đơn thuốc',
        type: 'success',
        onConfirm: () => navigation.replace('PrescriptionDetail', { id: prescription.id }),
      });
    } catch (e) {
      showAlert({
        title: 'Lỗi',
        message: e.response?.data?.message || e.message || 'Không thể thêm đơn thuốc',
        type: 'error',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={90}
    >
      <ScrollView style={styles.scroll} contentContainerStyle={styles.content}>
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Thông tin đơn thuốc</Text>
          <Text style={styles.label}>Tên đơn thuốc *</Text>
          <TextInput
            style={styles.input}
            value={title}
            onChangeText={setTitle}
            placeholder="vd: Đơn thuốc huyết áp"
          />
          <Text style={styles.label}>Bác sĩ kê</Text>
          <TextInput
            style={styles.input}
            value={doctorName}
            onChangeText={setDoctorName}
            placeholder="Tên bác sĩ"
          />
          <Text style={styles.label}>Từ ngày</Text>
          <TextInput
            style={styles.input}
            value={startDate}
            onChangeText={setStartDate}
            placeholder="YYYY-MM-DD"
          />
          <Text style={styles.label}>Đến ngày</Text>
          <TextInput
            style={styles.input}
            value={endDate}
            onChangeText={setEndDate}
            placeholder="YYYY-MM-DD"
          />
          <Text style={styles.label}>Ghi chú</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={notes}
            onChangeText={setNotes}
            placeholder="Ghi chú thêm..."
            multiline
            numberOfLines={2}
          />
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Thuốc đầu tiên (tùy chọn)</Text>
          <Text style={styles.hint}>Có thể thêm thuốc khác sau khi tạo đơn</Text>
          <Text style={styles.label}>Tên thuốc</Text>
          <TextInput
            style={styles.input}
            value={medName}
            onChangeText={setMedName}
            placeholder="vd: Paracetamol"
          />
          <Text style={styles.label}>Liều lượng</Text>
          <TextInput
            style={styles.input}
            value={medDosage}
            onChangeText={setMedDosage}
            placeholder="vd: 500mg x 2 viên"
          />
          <Text style={styles.label}>Giờ uống</Text>
          <TextInput
            style={styles.input}
            value={medTime}
            onChangeText={setMedTime}
            placeholder="08:00 hoặc 08:00:00"
          />
        </View>

        <TouchableOpacity
          style={[styles.btn, saving && styles.btnDisabled]}
          onPress={handleSave}
          disabled={saving}
        >
          {saving ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Tạo đơn thuốc</Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  scroll: { flex: 1 },
  content: { padding: 16, paddingBottom: 32 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  sectionTitle: { fontSize: 18, fontWeight: '600', color: '#0f766e', marginBottom: 12 },
  hint: { fontSize: 12, color: '#6b7280', marginBottom: 12 },
  label: { fontSize: 14, fontWeight: '600', color: '#374151', marginBottom: 8 },
  input: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
  },
  textArea: { minHeight: 60, textAlignVertical: 'top' },
  btn: {
    backgroundColor: '#0f766e',
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  btnDisabled: { opacity: 0.7 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});

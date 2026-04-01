import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, KeyboardAvoidingView, Platform } from 'react-native';
import { useAlert } from '../utils/showAlert';
import { createHealthEntry, updateHealthEntry } from '../api/health';

function numOrNull(s) {
  const t = (s ?? '').toString().trim();
  if (!t) return null;
  const n = Number(t);
  if (Number.isNaN(n)) return null;
  return n;
}

export default function HealthEntryFormScreen({ route, navigation }) {
  const { elderlyId, elderlyName, mode, entry } = route.params || {};
  const { showAlert } = useAlert();

  const [systolic, setSystolic] = useState(entry?.systolic?.toString?.() || '');
  const [diastolic, setDiastolic] = useState(entry?.diastolic?.toString?.() || '');
  const [heartRate, setHeartRate] = useState(entry?.heartRate?.toString?.() || '');
  const [bloodGlucose, setBloodGlucose] = useState(entry?.bloodGlucose?.toString?.() || '');
  const [temperature, setTemperature] = useState(entry?.temperature?.toString?.() || '');
  const [weight, setWeight] = useState(entry?.weight?.toString?.() || '');
  const [note, setNote] = useState(entry?.note || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    navigation.setOptions({ title: mode === 'edit' ? 'Sửa chỉ số' : 'Thêm chỉ số' });
  }, [mode, navigation]);

  const save = async () => {
    if (!elderlyId) return;
    setSaving(true);
    try {
      const payload = {
        systolic: numOrNull(systolic),
        diastolic: numOrNull(diastolic),
        heartRate: numOrNull(heartRate),
        bloodGlucose: numOrNull(bloodGlucose),
        temperature: numOrNull(temperature),
        weight: numOrNull(weight),
        note: note?.trim() || null,
      };

      if (mode === 'edit') {
        await updateHealthEntry(entry.id, payload);
      } else {
        await createHealthEntry(elderlyId, payload);
      }
      navigation.goBack();
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không lưu được', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={90}>
      <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>{elderlyName ? `Người cao tuổi: ${elderlyName}` : 'Nhập chỉ số'}</Text>

        <Row>
          <Field label="Huyết áp tâm thu (SYS)" value={systolic} onChange={setSystolic} placeholder="vd: 120" />
          <Field label="Huyết áp tâm trương (DIA)" value={diastolic} onChange={setDiastolic} placeholder="vd: 80" />
        </Row>
        <Row>
          <Field label="Nhịp tim (bpm)" value={heartRate} onChange={setHeartRate} placeholder="vd: 72" />
          <Field label="Đường huyết (mmol/L)" value={bloodGlucose} onChange={setBloodGlucose} placeholder="vd: 5.6" />
        </Row>
        <Row>
          <Field label="Nhiệt độ (°C)" value={temperature} onChange={setTemperature} placeholder="vd: 36.7" />
          <Field label="Cân nặng (kg)" value={weight} onChange={setWeight} placeholder="vd: 58.5" />
        </Row>

        <Text style={styles.label}>Ghi chú</Text>
        <TextInput
          style={[styles.input, styles.note]}
          value={note}
          onChangeText={setNote}
          placeholder="vd: đo trước ăn sáng..."
          multiline
          maxLength={500}
        />

        <TouchableOpacity style={[styles.btn, saving && styles.btnDisabled]} onPress={save} disabled={saving}>
          <Text style={styles.btnText}>{saving ? 'Đang lưu...' : 'Lưu'}</Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function Row({ children }) {
  return <View style={styles.row}>{children}</View>;
}

function Field({ label, value, onChange, placeholder }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        keyboardType="numeric"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { padding: 16, paddingBottom: 28 },
  title: { fontSize: 16, fontWeight: '800', color: '#0f766e', marginBottom: 12 },
  row: { flexDirection: 'row', gap: 10 },
  field: { flex: 1, marginBottom: 12 },
  label: { fontSize: 12, color: '#6b7280', marginBottom: 6, fontWeight: '700' },
  input: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, fontSize: 15 },
  note: { minHeight: 90, textAlignVertical: 'top' },
  btn: { marginTop: 12, backgroundColor: '#0f766e', padding: 14, borderRadius: 12, alignItems: 'center' },
  btnText: { color: '#fff', fontWeight: '800', fontSize: 16 },
  btnDisabled: { opacity: 0.7 },
});


import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Image,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as ImagePicker from 'expo-image-picker';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../context/AuthContext';
import { useAlert } from '../utils/showAlert';
import { getMessages, sendImage, sendText, WS_URL } from '../api/chat';

function safeParseFoods(json) {
  try {
    const obj = JSON.parse(json);
    const items = obj?.items || [];
    const names = items.map((it) => it?.name).filter(Boolean);
    const note = obj?.note;
    return { names, note };
  } catch {
    return null;
  }
}

export default function ChatScreen({ route, navigation }) {
  const { conversationId, title } = route.params || {};
  const { user } = useAuth();
  const { showAlert } = useAlert();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [input, setInput] = useState('');
  const flatRef = useRef(null);
  const clientRef = useRef(null);

  useEffect(() => {
    navigation.setOptions({ title: title || 'Chat' });
  }, [title, navigation]);

  const load = async () => {
    if (!conversationId) return;
    setLoading(true);
    try {
      const res = await getMessages(conversationId, 50);
      setList(res.data?.data || []);
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không tải được tin nhắn', type: 'error' });
    } finally {
      setLoading(false);
      setTimeout(() => flatRef.current?.scrollToEnd({ animated: false }), 0);
    }
  };

  useEffect(() => {
    load();
  }, [conversationId]);

  useEffect(() => {
    let active = true;

    const connect = async () => {
      try {
        const token = await AsyncStorage.getItem('token');
        if (!token || !conversationId) return;

        const client = new Client({
          reconnectDelay: 2000,
          webSocketFactory: () =>
            new WebSocket(WS_URL, undefined, {
              headers: { Authorization: `Bearer ${token}` },
            }),
          onConnect: () => {
            if (!active) return;
            client.subscribe(`/topic/conversations/${conversationId}`, (msg) => {
              try {
                const body = JSON.parse(msg.body);
                setList((prev) => {
                  const idx = prev.findIndex((m) => m.id === body.id);
                  if (idx >= 0) {
                    const next = [...prev];
                    next[idx] = body;
                    return next;
                  }
                  return [...prev, body];
                });
                setTimeout(() => flatRef.current?.scrollToEnd({ animated: true }), 0);
              } catch {}
            });
          },
          onStompError: () => {},
        });

        client.activate();
        clientRef.current = client;
      } catch {}
    };

    connect();
    return () => {
      active = false;
      try {
        clientRef.current?.deactivate();
      } catch {}
      clientRef.current = null;
    };
  }, [conversationId]);

  const renderItem = ({ item }) => {
    const isMe = item.senderId === user?.id;
    const parsed = item.aiFoodItemsJson ? safeParseFoods(item.aiFoodItemsJson) : null;
    const foodLine = parsed?.names?.length ? `Món ăn: ${parsed.names.join(', ')}` : null;
    const noteLine = parsed?.note || item.aiNote;

    return (
      <View style={[styles.row, isMe ? styles.rowMe : styles.rowOther]}>
        <View style={[styles.bubble, isMe ? styles.bubbleMe : styles.bubbleOther]}>
          {item.imageUrl ? (
            <Image source={{ uri: item.imageUrl }} style={styles.image} />
          ) : null}
          {item.text ? <Text style={[styles.text, isMe ? styles.textMe : styles.textOther]}>{item.text}</Text> : null}
          {foodLine ? <Text style={styles.ai}>{foodLine}</Text> : null}
          {noteLine ? <Text style={styles.aiNote}>{noteLine}</Text> : null}
          {item.createdAt ? (
            <Text style={styles.time}>{new Date(item.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</Text>
          ) : null}
        </View>
      </View>
    );
  };

  const upsertMessage = (msg) => {
    if (!msg?.id) return;
    setList((prev) => {
      const idx = prev.findIndex((m) => m.id === msg.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = msg;
        return next;
      }
      return [...prev, msg];
    });
    setTimeout(() => flatRef.current?.scrollToEnd({ animated: true }), 0);
  };

  const canSend = input.trim().length > 0 && !sending;

  const handleSend = async () => {
    const text = input.trim();
    if (!text || !conversationId) return;
    setSending(true);
    setInput('');
    try {
      const res = await sendText(conversationId, text);
      const dto = res?.data?.data;
      if (dto) upsertMessage(dto);
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không gửi được', type: 'error' });
    } finally {
      setSending(false);
    }
  };

  const pickImage = async () => {
    if (!conversationId || sending) return;
    try {
      const { status } = await ImagePicker.requestCameraPermissionsAsync();
      if (status !== 'granted') {
        showAlert({ title: 'Thiếu quyền', message: 'Cần quyền camera để chụp ảnh bữa ăn.', type: 'error' });
        return;
      }
      const result = await ImagePicker.launchCameraAsync({
        quality: 0.5,
      });
      if (result.canceled) return;
      const asset = result.assets?.[0];
      if (!asset?.uri) return;

      const fd = new FormData();
      fd.append('image', {
        uri: asset.uri,
        name: 'meal.jpg',
        type: 'image/jpeg',
      });
      if (input.trim()) fd.append('text', input.trim());

      setSending(true);
      setInput('');
      const res = await sendImage(conversationId, fd);
      const dto = res?.data?.data;
      if (dto) upsertMessage(dto);
    } catch (e) {
      showAlert({ title: 'Lỗi', message: e.response?.data?.message || 'Không gửi được ảnh', type: 'error' });
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#0f766e" />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={90}>
      <FlatList
        ref={flatRef}
        data={list}
        keyExtractor={(item) => String(item.id)}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        onContentSizeChange={() => flatRef.current?.scrollToEnd({ animated: false })}
      />
      <View style={styles.inputRow}>
        <TouchableOpacity style={[styles.camBtn, sending && styles.btnDisabled]} onPress={pickImage} disabled={sending}>
          <Text style={styles.camText}>📷</Text>
        </TouchableOpacity>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          placeholder="Nhắn tin..."
          placeholderTextColor="#9ca3af"
          multiline
          maxLength={2000}
          editable={!sending}
        />
        <TouchableOpacity style={[styles.sendBtn, (!canSend) && styles.btnDisabled]} onPress={handleSend} disabled={!canSend}>
          <Text style={styles.sendText}>{sending ? '...' : 'Gửi'}</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  list: { padding: 12, paddingBottom: 8 },
  row: { marginBottom: 10, flexDirection: 'row' },
  rowMe: { justifyContent: 'flex-end' },
  rowOther: { justifyContent: 'flex-start' },
  bubble: { maxWidth: '78%', borderRadius: 16, padding: 10 },
  bubbleMe: { backgroundColor: '#0f766e', borderBottomRightRadius: 4 },
  bubbleOther: { backgroundColor: '#fff', borderBottomLeftRadius: 4, borderWidth: 1, borderColor: '#e5e7eb' },
  text: { fontSize: 15, lineHeight: 22 },
  textMe: { color: '#fff' },
  textOther: { color: '#1f2937' },
  time: { fontSize: 10, color: '#9ca3af', marginTop: 6, textAlign: 'right' },
  image: { width: 220, height: 220, borderRadius: 12, marginBottom: 8, backgroundColor: '#e5e7eb' },
  ai: { marginTop: 6, fontSize: 12, color: '#111827', fontWeight: '700' },
  aiNote: { marginTop: 4, fontSize: 12, color: '#374151' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  inputRow: { flexDirection: 'row', alignItems: 'flex-end', padding: 12, paddingBottom: 20, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#e5e7eb', gap: 8 },
  camBtn: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  camText: { fontSize: 18 },
  input: { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 18, paddingHorizontal: 14, paddingVertical: 10, maxHeight: 100, backgroundColor: '#f9fafb' },
  sendBtn: { paddingHorizontal: 16, paddingVertical: 12, backgroundColor: '#0f766e', borderRadius: 18, minHeight: 44, justifyContent: 'center' },
  sendText: { color: '#fff', fontWeight: '700' },
  btnDisabled: { opacity: 0.5 },
});


# ElderCare App (Mobile + API)

Ứng dụng chăm sóc người cao tuổi với **chat realtime**, **nhắc thuốc + lịch sử uống thuốc**, **điểm danh hằng ngày**, **cảnh báo SOS/khẩn cấp**, **hồ sơ sức khoẻ theo mốc thời gian**, quản trị hệ thống (Admin) và **AI phân tích ảnh bữa ăn** (Google AI Studio / Gemini).

---

## Mục tiêu & lợi ích

- **Giảm rủi ro quên thuốc / bỏ bữa / bỏ điểm danh** nhờ nhắc nhở và lịch sử theo dõi.
- **Đồng bộ thời gian thực** giữa tài khoản người cao tuổi và người giám hộ (trạng thái điểm danh, uống thuốc, cảnh báo, chat).
- **Dễ vận hành**: cấu hình hệ thống thay đổi trực tiếp trong Admin (không cần sửa code).
- **Trải nghiệm phù hợp người cao tuổi**: UI chat có cỡ chữ lớn hơn cho tài khoản `ELDERLY`.

---

## Vai trò người dùng

- **ELDERLY (Người cao tuổi)**: điểm danh, xem đơn thuốc/lịch uống, xác nhận đã uống, chat với giám hộ, SOS.
- **CAREGIVER (Người giám hộ)**: quản lý đơn thuốc (tạo/sửa/xoá), theo dõi lịch sử uống thuốc, xem điểm danh, nhận cảnh báo, chat.
- **ADMIN**: quản lý người dùng, cấu hình hệ thống, theo dõi thống kê cơ bản.

---

## Tính năng chính

### Realtime (WebSocket STOMP)

- Endpoint: `ws://<host>:8082/ws`
- Broker topic prefix: `/topic/*`

Các topic đang dùng (đồng bộ realtime):
- **Chat**: `/topic/conversations/{conversationId}`
- **Điểm danh**: `/topic/checkins/{userId}`
- **Cảnh báo**: `/topic/alerts/{userId}`
- **Lịch sử uống thuốc**: `/topic/med-history/{userId}`

### Chat (text/ảnh) + AI phân tích bữa ăn

- Gửi tin nhắn text/ảnh (multipart).
- Ảnh được lưu trong thư mục `uploads/` và serve qua `/uploads/**`.
- AI phân tích ảnh bữa ăn và trả text/JSON vào `ai_food_items_json`/`ai_note` để hiển thị trong chat.

### Thuốc & đơn thuốc

- 1 đơn thuốc có **nhiều loại thuốc**.
- Lịch uống theo giờ/ngày trong tuần, nhắc trước `reminder_minutes_before`.
- Lịch sử uống thuốc: `PENDING/TAKEN/SKIPPED/MISSED` và đồng bộ realtime.

### Điểm danh & cảnh báo

- Người cao tuổi điểm danh; người giám hộ thấy trạng thái thay đổi ngay.
- Cảnh báo SOS và các cảnh báo khác cập nhật realtime.

### Hồ sơ sức khoẻ (Health timeline)

- Người giám hộ nhập các chỉ số (huyết áp, nhịp tim, đường huyết, cân nặng, nhiệt độ, ghi chú).
- Người cao tuổi xem timeline.

### Quản lý thiết bị đăng nhập

- Lưu token/thiết bị, xem danh sách thiết bị và revoke (tuỳ màn hình/triển khai).

---

## Thống kê API (Backend)

Base URL mặc định: `http://<host>:8082/api`

### Auth
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`

### Chat
- `GET /api/chat/conversations`
- `GET /api/chat/conversations/{id}/messages?limit=50`
- `POST /api/chat/conversations/{id}/messages`
- `POST /api/chat/conversations/{id}/messages/image` (multipart: `image`, optional `text`)

### Đơn thuốc
- `GET /api/prescriptions/elderly/{elderlyId}`
- `GET /api/prescriptions/{id}`
- `POST /api/prescriptions`
- `PUT /api/prescriptions/{id}`
- `POST /api/prescriptions/{id}/medications`
- `PUT /api/prescriptions/medications/{medicationId}`
- `POST /api/prescriptions/medications/{medicationId}/schedules`
- `PUT /api/prescriptions/schedules/{scheduleId}`

### Uống thuốc (history)
- `POST /api/medication-history/{scheduleId}/taken`
- `POST /api/medication-history/{scheduleId}/skip`
- `GET /api/medication-history/elderly/{elderlyId}`

### Điểm danh
- `POST /api/checkins`
- `GET /api/checkins/elderly/{elderlyId}`

### Cảnh báo
- `POST /api/alerts/sos`
- `GET /api/alerts/caregiver/{caregiverId}`

### Sức khoẻ
- `POST /api/health/elderly/{elderlyId}`
- `GET /api/health/elderly/{elderlyId}`

### Admin (yêu cầu role ADMIN)
- `GET /api/admin/stats`
- `GET /api/admin/users`
- `GET /api/admin/config`
- `PUT /api/admin/config`

---

## System Config (Admin)

Bảng `system_config` cho phép đổi cấu hình mà không cần redeploy.

Các key thường dùng:
- **`ai_provider`**: `google` / `openai`
- **`ai_google_api_key`**: API key Google AI Studio (Gemini Developer API)
- **`ai_google_model`**: ví dụ `gemini-2.5-flash` hoặc `gemini-3-flash-preview`
- **`ai_food_prompt_template`**: prompt template cho phân tích ảnh bữa ăn

Ghi chú:
- Google GenAI Java SDK ưu tiên env `GOOGLE_API_KEY` (legacy: `GEMINI_API_KEY`).

---

## Hướng dẫn chạy Database (MySQL)

1) Tạo DB + bảng:
- File: `database/schema.sql`
- Chạy trong MySQL client:

```sql
SOURCE database/schema.sql;
```

2) Tài khoản admin mặc định:
- email: `admin`
- password: `admin123`

---

## Hướng dẫn chạy Backend (Spring Boot)

### Yêu cầu
- Java **17+**
- Maven
- MySQL

### Cấu hình
Sửa `backend/src/main/resources/application.properties` (host/user/pass DB, JWT secret nếu có).

### Run (PowerShell)

```powershell
cd e:\DEV\appmobile\backend

$env:JAVA_HOME="C:\Program Files\ojdkbuild\java-17-openjdk-17.0.3.0.6-1"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

mvn -DskipTests clean package
java -jar .\target\eldercare-api-1.0.0.jar
```

Upload static:
- Ảnh chat lưu ở `backend/uploads/` (hoặc thư mục làm việc của process).
- Public URL: `http://<host>:8082/uploads/<file>`

---

## Hướng dẫn chạy Frontend (Expo React Native)

### Yêu cầu
- Node.js + npm

### Cài và chạy

```powershell
cd e:\DEV\appmobile\frontend
npm install
npx expo start
```

### Cấu hình API URL
Frontend đọc base URL theo ưu tiên:
1) `EXPO_PUBLIC_API_URL`
2) `app.json` → `expo.extra.apiUrl`

Ví dụ:

```powershell
$env:EXPO_PUBLIC_API_URL="http://192.168.1.244:8082/api"
```

---

## iOS Expo Go: tải ảnh `/uploads` không hiện (ATS)

Expo Go iOS có thể chặn HTTP khi tải ảnh. Cách xử lý nhanh:

### Dùng ngrok (khuyến nghị)

```powershell
ngrok http 8082
```

Lấy domain `https://xxxx.ngrok-free.dev` và set:

```powershell
$env:EXPO_PUBLIC_API_URL="https://xxxx.ngrok-free.dev/api"
```

Khi đó ảnh chat sẽ dùng `https://xxxx.ngrok-free.dev/uploads/...` và iOS sẽ hiển thị bình thường.

---

## Cấu trúc thư mục

- `backend/`: Spring Boot API + WebSocket + AI service
- `frontend/`: Expo React Native app
- `database/`: schema MySQL
- `diagrams/`: sơ đồ (nếu có)


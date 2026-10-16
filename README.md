# VieCultures Backend - Voice Generation Service

> 📌 **Dành cho AI Agent / Dev:** Tham khảo tài liệu kiến trúc & quy trình tạo API chuẩn tại [STRUCTURE.md](file:///c:/Edisk/dow/Tai_lieu/ky7/exe/viecultures/backend/STRUCTURE.md).

Dịch vụ sinh giọng đọc AI (Neural TTS) bằng Google Gemini API (hoặc Edge-TTS dự phòng) theo thiết kế trong `api.md`.

## 1. Cấu hình (`backend/.env`)

File [.env](file:///c:/Edisk/dow/Tai_lieu/ky7/exe/viecultures/backend/.env) chứa API Key của Google Gemini:

```env
# Google Gemini API Key


# Model hỗ trợ Audio (mặc định gemini-3.8-flash)
GEMINI_MODEL=gemini-3.8-flash

# Thư mục lưu audio (tự động xuất sang frontend để phát trực tiếp)
AUDIO_OUTPUT_DIR=../frontend/public/audio
```

> **Lưu ý về API Key hiện tại:**
> Key `AQ.Ab8RN6IxkLYocALzH61YQE4ja1l8Gqn31HSR3aEQ_FPcoXmuFA` là key Google AI Studio hợp lệ. Tuy nhiên dự án trên Google AI Studio hiện đang báo mã lỗi `402 Prepayment credits depleted`.
>
> - Để gọi Gemini trực tiếp, bạn chỉ cần nạp thêm credit hoặc tạo một API key mới từ project Free Tier trên https://aistudio.google.com/app/apikey và dán vào `backend/.env`.
> - Hệ thống backend đã tích hợp sẵn cơ chế **tự động fallback sang Edge-TTS** nên ngay cả khi Gemini API hết quota, server vẫn tự động sinh file âm thanh chất lượng cao không bị crash.

---

## 2. Các API Endpoints

### 2.1. Lấy danh sách giọng đọc có sẵn

```http
GET http://localhost:8080/api/voice/voices
```

**Response:**

```json
[
  {
    "id": "Puck",
    "name": "Puck",
    "gender": "Male",
    "desc": "Giọng Nam trầm, tự nhiên"
  },
  {
    "id": "Charon",
    "name": "Charon",
    "gender": "Male",
    "desc": "Giọng Nam trầm ấm, học thuật"
  },
  {
    "id": "Kore",
    "name": "Kore",
    "gender": "Female",
    "desc": "Giọng Nữ nhẹ nhàng, truyền cảm hứng"
  },
  {
    "id": "Fenrir",
    "name": "Fenrir",
    "gender": "Male",
    "desc": "Giọng Nam truyền cảm, rõ âm"
  },
  {
    "id": "Aoede",
    "name": "Aoede",
    "gender": "Female",
    "desc": "Giọng Nữ sang trọng, phát âm chuẩn mực"
  }
]
```

### 2.2. Sinh file voice cho bài viết mới

```http
POST http://localhost:8080/api/voice/generate
Content-Type: application/json

{
  "textContent": "Vietnam is a four-thousand-year-old river of resilience and shared bowls of fragrant broth.",
  "voiceName": "Puck",
  "outputFilename": "article_101_male.wav"
}
```

**Response:**

```json
{
  "success": true,
  "audioUrl": "/audio/article_101_male.wav",
  "filename": "article_101_male.wav",
  "voiceName": "Puck",
  "message": "Audio generated successfully",
  "timestamp": "2026-09-28T11:35:00"
}
```

File sau khi sinh sẽ nằm ngay trong `frontend/public/audio/article_101_male.wav` và web frontend có thể phát ngay với URL `/audio/article_101_male.wav`.

---

## 3. Chạy Backend Server

```bash
cd backend
mvn spring-boot:run
```

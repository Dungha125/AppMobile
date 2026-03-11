#!/usr/bin/env node

/**
 * Script để check IP hiện tại và so sánh với client.js
 * Chạy: node check-ip.js
 */

const os = require('os');
const fs = require('fs');
const path = require('path');

function getLocalIP() {
  const interfaces = os.networkInterfaces();
  const ips = [];
  
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      // Skip internal and non-IPv4 addresses
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push({
          interface: name,
          address: iface.address,
        });
      }
    }
  }
  
  return ips;
}

function getCurrentIPFromClientJs() {
  const clientJsPath = path.join(__dirname, 'src', 'api', 'client.js');
  
  try {
    const content = fs.readFileSync(clientJsPath, 'utf8');
    // Extract IP from: 'http://172.16.59.113:8082/api'
    const match = content.match(/http:\/\/([\d.]+):8082\/api/);
    if (match && match[1] !== '10.0.2.2' && match[1] !== 'localhost') {
      return match[1];
    }
  } catch (e) {
    console.error('❌ Không đọc được client.js:', e.message);
  }
  
  return null;
}

console.log('🔍 Checking IP Configuration...\n');

// Get system IPs
const localIPs = getLocalIP();

console.log('📡 IP addresses trên máy của bạn:');
if (localIPs.length === 0) {
  console.log('  ⚠️  Không tìm thấy IP nào (không kết nối mạng?)');
} else {
  localIPs.forEach((ip, index) => {
    console.log(`  ${index + 1}. ${ip.interface}: ${ip.address}`);
  });
}

const recommendedIP = localIPs.length > 0 ? localIPs[0].address : null;

console.log('');

// Get IP from client.js
const clientIP = getCurrentIPFromClientJs();

if (clientIP) {
  console.log(`📄 IP trong client.js: ${clientIP}`);
} else {
  console.log('📄 IP trong client.js: localhost hoặc emulator IP');
}

console.log('');

// Compare
if (clientIP && recommendedIP) {
  if (clientIP === recommendedIP) {
    console.log('✅ IP trong client.js ĐÚNG với IP máy hiện tại!');
  } else {
    console.log('❌ IP KHÔNG KHỚP!');
    console.log(`   Expected: ${recommendedIP}`);
    console.log(`   Found:    ${clientIP}`);
    console.log('');
    console.log('🔧 Cần update client.js:');
    console.log(`   Đổi: 'http://${clientIP}:8082/api'`);
    console.log(`   Thành: 'http://${recommendedIP}:8082/api'`);
  }
} else {
  console.log('ℹ️  Đang dùng localhost hoặc emulator IP - OK cho simulator/emulator');
  if (recommendedIP) {
    console.log(`💡 Nếu dùng thiết bị thật qua WiFi, dùng IP: ${recommendedIP}`);
  }
}

console.log('');
console.log('═══════════════════════════════════════');
console.log('');

if (recommendedIP) {
  console.log('📋 Cấu hình đề xuất cho client.js:');
  console.log('');
  console.log('const API_URL = Platform.OS === \'web\'');
  console.log('  ? \'http://localhost:8082/api\'');
  console.log('  : Platform.OS === \'android\'');
  console.log('    ? \'http://10.0.2.2:8082/api\'');
  console.log(`    : 'http://${recommendedIP}:8082/api';`);
  console.log('');
}

console.log('💡 Tips:');
console.log('  • Android Emulator: luôn dùng 10.0.2.2');
console.log('  • iOS Simulator: luôn dùng localhost');
console.log('  • Thiết bị thật (WiFi): dùng IP máy PC');
console.log('  • Nếu IP thay đổi thường xuyên: dùng tunnel mode');
console.log('    → npx expo start --tunnel');
console.log('');

// Backend check
console.log('🔍 Checking Backend...');
const http = require('http');

const req = http.get(`http://localhost:8082/api/auth/login`, (res) => {
  if (res.statusCode === 405 || res.statusCode === 400) {
    console.log('✅ Backend đang chạy trên port 8082');
  } else {
    console.log(`⚠️  Backend response với status: ${res.statusCode}`);
  }
}).on('error', (err) => {
  if (err.code === 'ECONNREFUSED') {
    console.log('❌ Backend KHÔNG chạy trên port 8082!');
    console.log('   → Chạy: cd backend && mvn spring-boot:run');
  } else {
    console.log(`⚠️  Lỗi kết nối: ${err.message}`);
  }
});

req.setTimeout(3000, () => {
  req.destroy();
  console.log('⚠️  Backend timeout - có thể đang khởi động');
});

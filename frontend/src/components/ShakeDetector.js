import { useEffect } from 'react';
import { useShakeDetection } from '../hooks/useShakeDetection';
import { useAuth } from '../context/AuthContext';
import passiveCheckInService from '../services/PassiveCheckInService';

/**
 * Component ẩn để phát hiện rung lắc (shake)
 * Tự động tạo PASSIVE check-in khi phát hiện shake
 */
export default function ShakeDetector() {
  const { user } = useAuth();

  useShakeDetection(() => {
    if (user && user.role === 'ELDERLY') {
      console.log('🤳 Shake detected for elderly user');
      passiveCheckInService.onShakeDetected(user.id);
    }
  }, {
    threshold: 15,  // Ngưỡng gia tốc (càng thấp = càng nhạy)
    timeout: 2000,  // 2 giây giữa các lần shake
  });

  return null; // Component ẩn, không render gì
}

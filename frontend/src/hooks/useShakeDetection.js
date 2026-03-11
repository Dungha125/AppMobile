import { useEffect, useRef } from 'react';
import { DeviceMotion } from 'expo-sensors';

/**
 * Hook để phát hiện rung lắc (shake) thiết bị
 * 
 * @param {Function} onShake - Callback khi phát hiện shake
 * @param {Object} options - Tùy chọn
 * @param {number} options.threshold - Ngưỡng gia tốc (default: 15)
 * @param {number} options.timeout - Thời gian debounce giữa các lần shake (default: 1000ms)
 */
export const useShakeDetection = (onShake, options = {}) => {
  const {
    threshold = 15, // Ngưỡng gia tốc để coi là shake
    timeout = 1000, // 1 giây giữa các lần shake
  } = options;

  const lastShakeTime = useRef(0);
  const subscription = useRef(null);

  useEffect(() => {
    let mounted = true;

    const startListening = async () => {
      try {
        // Check quyền truy cập accelerometer
        const { status } = await DeviceMotion.requestPermissionsAsync();
        if (status !== 'granted') {
          console.log('⚠️ Accelerometer permission not granted');
          return;
        }

        // Set update interval (100ms)
        DeviceMotion.setUpdateInterval(100);

        // Subscribe to device motion
        subscription.current = DeviceMotion.addListener((data) => {
          if (!mounted) return;

          const { x, y, z } = data.acceleration || {};
          if (x == null || y == null || z == null) return;

          // Tính total acceleration
          const totalAcceleration = Math.sqrt(x * x + y * y + z * z);

          // Phát hiện shake
          if (totalAcceleration > threshold) {
            const now = Date.now();
            
            // Debounce - chỉ trigger nếu đã quá timeout từ lần shake trước
            if (now - lastShakeTime.current > timeout) {
              lastShakeTime.current = now;
              console.log(`🤳 Shake detected! Acceleration: ${totalAcceleration.toFixed(2)}`);
              
              if (onShake) {
                onShake();
              }
            }
          }
        });

        console.log('✓ Shake detection started');
      } catch (error) {
        console.error('Error starting shake detection:', error);
      }
    };

    startListening();

    // Cleanup
    return () => {
      mounted = false;
      if (subscription.current) {
        subscription.current.remove();
        console.log('✓ Shake detection stopped');
      }
    };
  }, [onShake, threshold, timeout]);

  return null;
};

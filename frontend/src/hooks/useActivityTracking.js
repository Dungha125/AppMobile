import { useEffect, useCallback, useRef } from 'react';
import { AppState } from 'react-native';
import passiveCheckInService from '../services/PassiveCheckInService';

/**
 * Hook để theo dõi hoạt động user trong app
 * Tự động tạo PASSIVE check-in khi:
 * 1. Mở app
 * 2. Có đủ hoạt động trong 5 phút
 * 
 * @param {Object} user - User object (phải có id và role)
 * @param {boolean} enabled - Bật/tắt tracking
 */
export const useActivityTracking = (user, enabled = true) => {
  const appState = useRef(AppState.currentState);
  const hasTrackedAppOpen = useRef(false);

  // Track app open/foreground
  useEffect(() => {
    if (!enabled || !user || user.role !== 'ELDERLY') return;

    // Track khi component mount (app open lần đầu)
    if (!hasTrackedAppOpen.current) {
      hasTrackedAppOpen.current = true;
      passiveCheckInService.onAppOpen(user.id);
    }

    // Listen to app state changes
    const subscription = AppState.addEventListener('change', (nextAppState) => {
      // App chuyển từ background → foreground
      if (appState.current.match(/inactive|background/) && nextAppState === 'active') {
        console.log('📱 App came to foreground');
        passiveCheckInService.onAppOpen(user.id);
      }
      
      appState.current = nextAppState;
    });

    return () => {
      subscription.remove();
    };
  }, [user, enabled]);

  // Check daily reset
  useEffect(() => {
    if (!enabled) return;

    const interval = setInterval(() => {
      passiveCheckInService.checkDailyReset();
    }, 60000); // Check mỗi phút

    return () => clearInterval(interval);
  }, [enabled]);

  /**
   * Gọi function này khi user có bất kỳ tương tác nào:
   * - Bấm nút
   * - Scroll
   * - Navigate
   * - Input text
   */
  const trackActivity = useCallback(() => {
    if (!enabled || !user || user.role !== 'ELDERLY') return;
    passiveCheckInService.trackActivity(user.id);
  }, [user, enabled]);

  return { trackActivity };
};

/**
 * HOC để tự động track activity cho một component
 */
export const withActivityTracking = (Component) => {
  return (props) => {
    const { trackActivity } = useActivityTracking(props.user, true);
    
    return (
      <Component 
        {...props} 
        trackActivity={trackActivity}
      />
    );
  };
};

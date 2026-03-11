import AsyncStorage from '@react-native-async-storage/async-storage';
import api from '../api/client';

/**
 * Service quản lý PASSIVE check-in (tự động)
 * Triggers:
 * 1. Mở app
 * 2. Rung lắc device (shake)
 * 3. Hoạt động liên tục trong 5 phút
 */

const STORAGE_KEYS = {
  LAST_PASSIVE_CHECKIN: 'lastPassiveCheckInDate',
  LAST_ACTIVITY: 'lastActivityTimestamp',
  ACTIVITY_COUNT: 'todayActivityCount',
};

const THRESHOLDS = {
  MIN_ACTIVITIES_FOR_CHECKIN: 10, // 10 tương tác trong 5 phút
  ACTIVITY_WINDOW_MS: 5 * 60 * 1000, // 5 phút
};

class PassiveCheckInService {
  constructor() {
    this.activityCount = 0;
    this.lastActivityTime = null;
    this.isProcessing = false;
  }

  /**
   * Kiểm tra và tạo PASSIVE check-in nếu chưa có hôm nay
   */
  async tryCreatePassiveCheckIn(userId, reason = 'Tự động phát hiện hoạt động') {
    if (this.isProcessing) return;

    try {
      this.isProcessing = true;

      // Check đã tạo PASSIVE hôm nay chưa
      const today = new Date().toISOString().split('T')[0];
      const lastPassive = await AsyncStorage.getItem(STORAGE_KEYS.LAST_PASSIVE_CHECKIN);

      if (lastPassive === today) {
        console.log('⚠️ PASSIVE check-in already exists today');
        return false;
      }

      // Tạo PASSIVE check-in
      console.log('🔵 Creating PASSIVE check-in:', reason);
      
      const response = await api.post('/check-ins', {
        elderlyId: userId,
        type: 'PASSIVE',
        notes: reason,
      });

      if (response.data?.success) {
        await AsyncStorage.setItem(STORAGE_KEYS.LAST_PASSIVE_CHECKIN, today);
        console.log('✓ PASSIVE check-in created successfully');
        return true;
      }

      return false;
    } catch (error) {
      // Silent fail - không làm phiền user
      if (error.response?.status === 400 && error.response?.data?.message?.includes('đã điểm danh')) {
        console.log('⚠️ Already checked in today (ACTIVE)');
        // Mark as done để không thử lại
        const today = new Date().toISOString().split('T')[0];
        await AsyncStorage.setItem(STORAGE_KEYS.LAST_PASSIVE_CHECKIN, today);
      } else {
        console.log('⚠️ PASSIVE check-in failed:', error.message);
      }
      return false;
    } finally {
      this.isProcessing = false;
    }
  }

  /**
   * Gọi khi mở app
   */
  async onAppOpen(userId) {
    if (!userId) return;
    console.log('📱 App opened - trying PASSIVE check-in');
    await this.tryCreatePassiveCheckIn(userId, 'Người dùng mở ứng dụng');
  }

  /**
   * Gọi khi phát hiện rung lắc
   */
  async onShakeDetected(userId) {
    if (!userId) return;
    console.log('🤳 Shake detected - trying PASSIVE check-in');
    await this.tryCreatePassiveCheckIn(userId, 'Phát hiện người dùng rung lắc thiết bị');
  }

  /**
   * Theo dõi hoạt động (gọi khi user tương tác)
   */
  async trackActivity(userId) {
    if (!userId) return;

    const now = Date.now();
    
    // Reset activity count nếu quá 5 phút không hoạt động
    if (this.lastActivityTime && (now - this.lastActivityTime) > THRESHOLDS.ACTIVITY_WINDOW_MS) {
      this.activityCount = 0;
    }

    this.activityCount++;
    this.lastActivityTime = now;

    console.log(`📊 Activity tracked: ${this.activityCount}/${THRESHOLDS.MIN_ACTIVITIES_FOR_CHECKIN}`);

    // Nếu đủ 10 hoạt động trong 5 phút → PASSIVE check-in
    if (this.activityCount >= THRESHOLDS.MIN_ACTIVITIES_FOR_CHECKIN) {
      console.log('✓ Enough activities detected in 5 minutes');
      const success = await this.tryCreatePassiveCheckIn(
        userId, 
        `Phát hiện ${this.activityCount} hoạt động trong 5 phút`
      );
      
      if (success) {
        this.activityCount = 0; // Reset counter
      }
    }
  }

  /**
   * Reset counter (gọi khi đổi ngày)
   */
  async resetDaily() {
    this.activityCount = 0;
    this.lastActivityTime = null;
    console.log('🔄 Daily reset for PASSIVE check-in tracking');
  }

  /**
   * Check nếu cần reset (gọi định kỳ)
   */
  async checkDailyReset() {
    const today = new Date().toISOString().split('T')[0];
    const lastDate = await AsyncStorage.getItem(STORAGE_KEYS.LAST_PASSIVE_CHECKIN);
    
    if (lastDate && lastDate !== today) {
      await this.resetDaily();
    }
  }
}

// Singleton instance
const passiveCheckInService = new PassiveCheckInService();

export default passiveCheckInService;

import React from 'react';
import { TouchableWithoutFeedback, View } from 'react-native';
import { useActivityTracking } from '../hooks/useActivityTracking';
import { useAuth } from '../context/AuthContext';

/**
 * Wrapper component để track user activity
 * Wrap toàn bộ app để detect mọi touch/interaction
 */
export default function ActivityTracker({ children }) {
  const { user } = useAuth();
  const { trackActivity } = useActivityTracking(user, user?.role === 'ELDERLY');

  return (
    <TouchableWithoutFeedback onPress={trackActivity}>
      <View style={{ flex: 1 }}>
        {children}
      </View>
    </TouchableWithoutFeedback>
  );
}

package com.abhaythemaster.discordmc.util;

import java.util.HashMap;
import java.util.Map;

public class Anim {
    private static final Map<String, Float> values = new HashMap<>();
    private static final Map<String, Long> lastTick = new HashMap<>();

    // Smooth lerp toward target (0.0 - 1.0)
    public static float lerp(String key, float target, float speed) {
        float current = values.getOrDefault(key, 0f);
        long now = System.currentTimeMillis();
        long last = lastTick.getOrDefault(key, now);
        float dt = Math.min((now - last) / 1000f, 0.1f);
        current += (target - current) * Math.min(1f, speed * dt * 60f);
        values.put(key, current);
        lastTick.put(key, now);
        return current;
    }

    // Pulse animation (0.0 - 1.0 sine wave)
    public static float pulse(float speed) {
        return (float)(Math.sin(System.currentTimeMillis() / 1000.0 * speed * Math.PI) * 0.5 + 0.5);
    }

    public static void reset(String key) { values.remove(key); lastTick.remove(key); }
}

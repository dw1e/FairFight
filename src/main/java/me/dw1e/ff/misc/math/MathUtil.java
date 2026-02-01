package me.dw1e.ff.misc.math;

import org.bukkit.util.Vector;

import java.util.Collection;

public final class MathUtil {

    public static final double EXPANDER = Math.pow(2.0, 24.0);

    public static double hypot(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static double gcd(double a, double b) {
        long expansionA = (long) (Math.abs(a) * EXPANDER), expansionB = (long) (Math.abs(b) * EXPANDER);

        return getGcd(expansionA, expansionB) / EXPANDER;
    }

    public static long getGcd(long current, long previous) {
        return previous <= 16384L ? current : getGcd(previous, current % previous);
    }

    public static double mean(Collection<? extends Number> samples) {
        if (samples.isEmpty()) return 0.0;

        double sum = 0.0;

        for (Number val : samples) sum += val.doubleValue();

        return sum / samples.size();
    }

    public static double variance(Collection<? extends Number> samples) {
        if (samples.size() <= 1) return 0.0;

        double sumSquaredDiff = 0.0;

        for (Number val : samples) {
            double diff = val.doubleValue() - mean(samples);

            sumSquaredDiff += diff * diff;
        }

        return sumSquaredDiff / (samples.size() - 1);
    }

    public static double deviation(Collection<? extends Number> samples) {
        return Math.sqrt(variance(samples));
    }

    public static float wrapAngleTo180(float angle) {
        angle %= 360.0F;

        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;

        return angle;
    }

    public static float normalizeYaw(float yaw) {
        float rot = (yaw - 90.0F) % 360.0F;

        if (rot < 0.0F) rot += 360.0F;

        return rot;
    }

    public static double distBetweenAngles360(double angle1, double angle2) {
        double abs = Math.abs(angle1 % 360.0 - angle2 % 360.0);

        return Math.abs(Math.min(360.0 - abs, abs));
    }

    public static double angle(Vector a, Vector b) {
        double dot = Math.min(Math.max(a.dot(b) / (a.length() * b.length()), -1), 1);
        return Math.acos(dot);
    }
}

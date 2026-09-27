/** IEEE-754 binary16 helpers for the archive research tables. */
final class HalfFloat {
    private HalfFloat() {}

    static String formatLE(byte[] bytes) {
        if (bytes == null || bytes.length != 2) return "";
        int bits = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
        return Float.toString(toFloat(bits));
    }

    static byte[] parseLE(String text) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Enter a Float16 value.");
        float value;
        try { value = Float.parseFloat(text.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid Float16 value: " + text); }
        if (Float.isFinite(value) && Math.abs(value) > 65504.0f)
            throw new IllegalArgumentException("Finite Float16 range is -65504 to 65504.");
        int bits = fromFloat(value);
        return new byte[]{(byte) bits, (byte) (bits >>> 8)};
    }

    static float toFloat(int half) {
        int sign = (half >>> 15) & 1;
        int exp = (half >>> 10) & 0x1F;
        int mant = half & 0x3FF;
        float value;
        if (exp == 0) {
            value = mant == 0 ? 0.0f : Math.scalb((float) mant, -24);
        } else if (exp == 31) {
            value = mant == 0 ? Float.POSITIVE_INFINITY : Float.NaN;
        } else {
            value = Math.scalb(1.0f + mant / 1024.0f, exp - 15);
        }
        return sign == 0 ? value : -value;
    }

    static int fromFloat(float value) {
        int bits = Float.floatToRawIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exp = (bits >>> 23) & 0xFF;
        int mant = bits & 0x7FFFFF;

        if (exp == 0xFF) {
            if (mant == 0) return sign | 0x7C00;
            return sign | 0x7E00; // canonical quiet NaN
        }

        int halfExp = exp - 127 + 15;
        if (halfExp >= 31) return sign | 0x7C00;
        if (halfExp <= 0) {
            if (halfExp < -10) return sign;
            mant |= 0x800000;
            int shift = 14 - halfExp;
            int halfMant = mant >>> shift;
            int remainder = mant & ((1 << shift) - 1);
            int halfway = 1 << (shift - 1);
            if (remainder > halfway || (remainder == halfway && (halfMant & 1) != 0)) halfMant++;
            return sign | halfMant;
        }

        int halfMant = mant >>> 13;
        int remainder = mant & 0x1FFF;
        if (remainder > 0x1000 || (remainder == 0x1000 && (halfMant & 1) != 0)) {
            halfMant++;
            if (halfMant == 0x400) {
                halfMant = 0;
                halfExp++;
                if (halfExp >= 31) return sign | 0x7C00;
            }
        }
        return sign | (halfExp << 10) | (halfMant & 0x3FF);
    }
}

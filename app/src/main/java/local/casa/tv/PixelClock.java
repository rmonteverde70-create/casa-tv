package local.casa.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextClock;

final class PixelClock extends TextClock {
    private static final int[][] DIGITS = {
        {14, 17, 17, 17, 17, 17, 14},
        {4, 12, 4, 4, 4, 4, 14},
        {14, 17, 1, 2, 4, 8, 31},
        {30, 1, 1, 14, 1, 1, 30},
        {2, 6, 10, 18, 31, 2, 2},
        {31, 16, 16, 30, 1, 1, 30},
        {14, 16, 16, 30, 17, 17, 14},
        {31, 1, 2, 4, 8, 8, 8},
        {14, 17, 17, 14, 17, 17, 14},
        {14, 17, 17, 15, 1, 1, 14},
    };

    private final Paint pixel = new Paint();

    PixelClock(Context context) {
        super(context);
        setFormat12Hour("HH:mm:ss");
        setFormat24Hour("HH:mm:ss");
    }

    @Override protected void onDraw(Canvas canvas) {
        CharSequence value = getText();
        if (value.length() != 8) return;
        float unit = Math.min(getWidth() / 38f, getHeight() / 7.45f);
        float small = unit * 0.57f;
        float width = 25.6f * unit + 2.4f * small + 10.6f * small;
        float x = (getWidth() - width) / 2f;
        float y = (getHeight() - 6.76f * unit) / 2f;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean seconds = index >= 5;
            float scale = seconds ? small : unit;
            float top = seconds ? y + 6.76f * (unit - small) : y;
            pixel.setColor(seconds ? 0xffc5c1bc : 0xfff5f2ec);
            if (character == ':') {
                drawPixel(canvas, x + 0.15f * scale, top + 2 * scale, scale);
                drawPixel(canvas, x + 0.15f * scale, top + 4 * scale, scale);
                x += 2.4f * scale;
            } else {
                int digit = Character.digit(character, 10);
                if (digit < 0) return;
                for (int row = 0; row < 7; row++) {
                    for (int column = 0; column < 5; column++) {
                        if ((DIGITS[digit][row] & (16 >> column)) != 0) {
                            drawPixel(canvas, x + column * scale, top + row * scale, scale);
                        }
                    }
                }
                x += 5.8f * scale;
            }
        }
    }

    private void drawPixel(Canvas canvas, float x, float y, float scale) {
        float size = scale * 0.76f;
        canvas.drawRect(x, y, x + size, y + size, pixel);
    }
}

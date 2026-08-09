package top.niunaijun.blackbox.core;

import androidx.annotation.Keep;

@Keep
public interface DrawingCallback {
    void drawLine(float x1, float y1, float x2, float y2, int color, float thickness);
    void drawBox(float x, float y, float w, float h, int color, float thickness, boolean isFilled);
    void drawText(String text, float x, float y, int color, float size, boolean isCentered);
    void drawCircle(float x, float y, float r, int color, float thickness, boolean isFilled);
    void clearDrawings();
}

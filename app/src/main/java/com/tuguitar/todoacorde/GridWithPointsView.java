package com.tuguitar.todoacorde;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridWithPointsView extends View {
    private Paint gridPaint;
    private Paint pointPaint;
    private Paint thickTopLinePaint;
    private Paint textPaint;
    private Paint symbolPaint; // Paint for 'X' and '0' symbols
    private Paint fingerNumberPaint; // Paint for finger numbers
    private int[] pointPositions;
    private String[] stringSymbols; // Array to hold 'X' and '0' symbols
    private String[] fingerNumbers; // Array to hold finger numbers
    private int leftNumber;

    public GridWithPointsView(Context context) {
        super(context);
        init();
    }

    public GridWithPointsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridWithPointsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(0xFF000000); // Black color for grid lines
        gridPaint.setStrokeWidth(4); // Standard thickness for grid lines

        thickTopLinePaint = new Paint();
        thickTopLinePaint.setColor(0xFF000000); // Black color for the top line
        thickTopLinePaint.setStrokeWidth(12); // Thicker line for the topmost horizontal line

        pointPaint = new Paint();
        pointPaint.setColor(0xFF000000); // Black color for points
        pointPaint.setStyle(Paint.Style.FILL); // Fill the points
        pointPaint.setStrokeWidth(12); // Thicker points

        textPaint = new Paint();
        textPaint.setColor(0xFF000000); // Black color for text
        textPaint.setTextSize(40); // Size of the number
        textPaint.setTextAlign(Paint.Align.CENTER); // Center align the text for the left number

        symbolPaint = new Paint();
        symbolPaint.setColor(0xFF000000); // Black color for symbols
        symbolPaint.setTextSize(40); // Set the initial size to be used by both 'X' and '0'
        symbolPaint.setTextAlign(Paint.Align.CENTER); // Align symbols to the center of the string

        fingerNumberPaint = new Paint();
        fingerNumberPaint.setColor(0xFFFFFFFF); // White color for finger numbers
        fingerNumberPaint.setTextSize(24); // Adjust size for finger numbers
        fingerNumberPaint.setTextAlign(Paint.Align.CENTER); // Align text to the center of the point

        pointPositions = new int[]{-1, -1, -1, -1, -1, -1}; // Initialize with no points
        stringSymbols = new String[]{"", "", "", "", "", ""}; // Initialize with empty symbols
        fingerNumbers = new String[]{"", "", "", "", "", ""}; // Initialize with empty finger numbers
        leftNumber = 1; // Default value
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int numRows = 5; // Adjust as needed for your grid
        int numColumns = 5; // Make the grid rectangular by adding more columns

        // Calculate the distance between the lines
        int rowSpacing = height / numRows;
        int colSpacing = width / numColumns;

        // Draw the topmost thick horizontal line if leftNumber is 1
        if (leftNumber == 1) {
            canvas.drawLine(0, 0, width, 0, thickTopLinePaint);
        }

        // Draw the standard leftmost vertical line (thin)
        canvas.drawLine(0, 0, 0, height, gridPaint);

        // Draw the remaining grid lines, skipping the bottom horizontal line
        for (int i = 1; i < numRows; i++) {
            canvas.drawLine(0, i * rowSpacing, width, i * rowSpacing, gridPaint);
        }
        for (int i = 1; i < numColumns; i++) {
            canvas.drawLine(i * colSpacing, 0, i * colSpacing, height, gridPaint);
        }

        // Draw points and finger numbers based on pointPositions and fingerNumbers arrays
        for (int i = 0; i < pointPositions.length; i++) {
            if (pointPositions[i] != -1) {
                float cx = i * colSpacing;  // Align with vertical lines
                float cy = (pointPositions[i] - 0.5f) * rowSpacing; // Shift the point slightly above the intersection
                canvas.drawCircle(cx, cy, 15, pointPaint); // Larger and thicker point

                // Draw the finger number inside the point
                if (!fingerNumbers[i].isEmpty()) {
                    canvas.drawText(fingerNumbers[i], cx, cy + 8, fingerNumberPaint); // Adjust the y-position to center text
                }
            }
        }

        // Draw the symbols ('X' and '0') above the topmost horizontal line
        for (int i = 0; i < stringSymbols.length; i++) {
            if (!stringSymbols[i].isEmpty()) {
                float cx = i * colSpacing;  // Align with vertical lines
                float cy = -20; // Position just above the topmost line

                if (stringSymbols[i].equals("0")) {
                    symbolPaint.setTextSize(40); // Smaller size for '0'
                } else {
                    symbolPaint.setTextSize(60); // Larger size for 'X'
                }

                canvas.drawText(stringSymbols[i], cx, cy, symbolPaint);
            }
        }

        // Draw the right border line (but skip the bottom border line)
        canvas.drawLine(width, 0, width, height, gridPaint); // Right border

        // Draw the number on the top-left side of the grid, centered in the first rectangle
        float xPos = -50;  // Adjust the x-position to move it left and away from the grid
        float yPos = rowSpacing / 2 + 10;  // Adjust the y-position to center the number within the first row rectangle
        canvas.drawText(String.valueOf(leftNumber), xPos, yPos, textPaint);
    }

    public void setPointsFromHint(String hint, String fingers) {
        boolean needsAdjustment = false;
        int minValue = Integer.MAX_VALUE;

        // First pass: find the minimum fret greater than 0 and check if adjustment is needed
        for (int i = 0; i < hint.length(); i++) {
            char symbol = hint.charAt(i);
            if (symbol != 'x' && symbol != '0') {
                int fret = Character.getNumericValue(symbol);
                if (fret > 5) {
                    needsAdjustment = true;
                }
                if (fret > 0 && fret < minValue) {
                    minValue = fret;
                }
            }
        }

        leftNumber = needsAdjustment ? minValue : 1; // Adjust only if needed

        // Second pass: adjust the point positions and set symbols based on the minimum fret
        for (int i = 0; i < hint.length(); i++) {
            char symbol = hint.charAt(i);
            if (symbol == 'x' || symbol == '0') {
                pointPositions[i] = -1; // No point for this string
                stringSymbols[i] = String.valueOf(symbol); // Store the 'X' or '0' symbol
                fingerNumbers[i] = ""; // No finger number for 'X' or '0'
            } else {
                int fret = Character.getNumericValue(symbol);
                pointPositions[i] = (fret > 0 && needsAdjustment) ? fret - (minValue - 1) : fret;
                stringSymbols[i] = ""; // No symbol for this string
                fingerNumbers[i] = fingers.length() > i ? String.valueOf(fingers.charAt(i)) : ""; // Store the finger number if provided
            }
        }

        invalidate(); // Redraw the view
    }
}

package com.tuguitar.todoacorde;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScaleFretboardView extends View {

    private static final String TAG = "ScaleFretboardView";

    private final Paint stringPaint = new Paint();
    private final Paint fretPaint = new Paint();
    private final Paint nutPaint = new Paint();
    private final Paint degreePaint = new Paint(); // para nombre dentro del punto
    private final Paint fretNumberPaint = new Paint();
    private final Paint highlightBackgroundPaint = new Paint();

    private final int numStrings = 6;
    private final int numFrets = 13;
    private final float noteRadius = 30f; // aumentado para más presencia
    private final float highlightHaloExtra = 16f; // grosor del halo de resaltado
    private float verticalBasePaddingPx;
    private float extraBottomForFretNumbers;
    private float fretSpacing;
    private float stringSpacing;
    private float extraLeftPadding; // para que el halo de la nota al aire no se corte

    private List<ScaleFretNote> scaleNotes = new ArrayList<>();

    // secuencia de resaltado
    private List<ScaleFretNote> highlightSequence = new ArrayList<>();
    private int highlightIndex = -1;
    private final Map<String, Integer> positionToSequenceIndex = new HashMap<>();

    public ScaleFretboardView(Context context) {
        super(context);
        init();
    }

    public ScaleFretboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScaleFretboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        stringPaint.setColor(0xFF222222);
        stringPaint.setStrokeWidth(4f);
        stringPaint.setAntiAlias(true);

        fretPaint.setColor(0xFFCCCCCC);
        fretPaint.setStrokeWidth(2f);
        fretPaint.setAntiAlias(true);

        nutPaint.setColor(0xFF000000);
        nutPaint.setStrokeWidth(10f);
        nutPaint.setAntiAlias(true);

        degreePaint.setColor(Color.WHITE);
        degreePaint.setTextAlign(Paint.Align.CENTER);
        degreePaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        degreePaint.setFakeBoldText(true);
        degreePaint.setAntiAlias(true);

        fretNumberPaint.setColor(0xFF444444);
        fretNumberPaint.setTextAlign(Paint.Align.CENTER);
        fretNumberPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        fretNumberPaint.setFakeBoldText(false);
        fretNumberPaint.setAntiAlias(true);

        // halo de resaltado: amarillo más opaco y grande, se dibuja detrás
        highlightBackgroundPaint.setColor(Color.argb(0xCC, 0xFF, 0xEB, 0x3B)); // opacidad alta pero semi
        highlightBackgroundPaint.setStyle(Paint.Style.FILL);
        highlightBackgroundPaint.setAntiAlias(true);

        verticalBasePaddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                getResources().getDisplayMetrics()
        );
        extraBottomForFretNumbers = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                28,
                getResources().getDisplayMetrics()
        );
        extraLeftPadding = noteRadius + highlightHaloExtra + 6f; // espacio extra para halo
    }

    public void setScaleNotes(List<ScaleFretNote> notes) {
        this.scaleNotes = notes != null ? notes : new ArrayList<>();
        Log.i(TAG, "setScaleNotes: count=" + this.scaleNotes.size());
        invalidate();
    }

    public void setHighlightSequence(List<ScaleFretNote> sequence) {
        if (sequence == null) {
            highlightSequence = new ArrayList<>();
            highlightIndex = -1;
            positionToSequenceIndex.clear();
            Log.i(TAG, "Highlight sequence cleared.");
            invalidate();
            return;
        }
        List<ScaleFretNote> sorted = new ArrayList<>(sequence);
        // recorrido: cuerda 6ª a 1ª, dentro de cada cuerda traste ascendente
        sorted.sort(Comparator
                .comparingInt((ScaleFretNote n) -> n.stringIndex)
                .thenComparingInt(n -> n.fret));
        highlightSequence = sorted;
        highlightIndex = 0;
        rebuildPositionMap();
        Log.i(TAG, "Highlight sequence set. size=" + highlightSequence.size());
        invalidate();
    }

    private void rebuildPositionMap() {
        positionToSequenceIndex.clear();
        for (int i = 0; i < highlightSequence.size(); i++) {
            ScaleFretNote note = highlightSequence.get(i);
            positionToSequenceIndex.put(keyFor(note.stringIndex, note.fret), i);
        }
    }

    private String keyFor(int stringIndex, int fret) {
        return stringIndex + ":" + fret;
    }

    public void setHighlightIndex(int idx) {
        if (highlightSequence == null || highlightSequence.isEmpty()) return;
        if (idx >= 0 && idx < highlightSequence.size()) {
            highlightIndex = idx;
            Log.i(TAG, "Highlight index set to " + idx);
            invalidate();
        } else {
            Log.w(TAG, "Invalid highlightIndex " + idx + " size=" + getHighlightSequenceSize());
        }
    }

    public int getHighlightSequenceSize() {
        return highlightSequence == null ? 0 : highlightSequence.size();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float usableWidth = width - extraLeftPadding;
        fretSpacing = usableWidth / numFrets;
        stringSpacing = fretSpacing * 0.75f; // más separación vertical para radio mayor
        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + 6f);
        int height = (int) (topOffset + (numStrings - 1) * stringSpacing + extraBottomForFretNumbers);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padL = getPaddingLeft();
        float availableWidth = getWidth() - padL - getPaddingRight();
        float usableWidth = availableWidth - extraLeftPadding;
        fretSpacing = usableWidth / numFrets;
        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + 6f);
        float boardHeight = (numStrings - 1) * stringSpacing;
        float effectiveLeft = padL + extraLeftPadding;

        // 1. Cuerdas
        for (int i = 0; i < numStrings; i++) {
            float y = topOffset + (numStrings - 1 - i) * stringSpacing;
            canvas.drawLine(effectiveLeft, y, effectiveLeft + numFrets * fretSpacing, y, stringPaint);
        }

        // 2. Trastes
        for (int i = 0; i <= numFrets; i++) {
            float x = effectiveLeft + i * fretSpacing;
            canvas.drawLine(x, topOffset, x, topOffset + boardHeight, i == 0 ? nutPaint : fretPaint);
        }

        // 3. Marcadores de traste
        float markerRadius = 7f;
        int[] markerFrets = {3, 5, 7, 9, 12};
        for (int fret : markerFrets) {
            float x = effectiveLeft + fret * fretSpacing - fretSpacing / 2;
            float y = topOffset + boardHeight / 2f;
            canvas.drawCircle(x, y, markerRadius, fretPaint);
        }

        // 4. Notas
        for (ScaleFretNote note : scaleNotes) {
            int stringIdx = note.stringIndex;
            int fret = note.fret;

            float y = topOffset + (numStrings - 1 - stringIdx) * stringSpacing;
            float x;
            if (fret == 0) {
                x = effectiveLeft;
            } else {
                x = effectiveLeft + fret * fretSpacing - fretSpacing / 2;
            }

            String key = keyFor(stringIdx, fret);
            boolean isCurrentHighlight = (highlightSequence != null
                    && highlightIndex >= 0
                    && highlightIndex < highlightSequence.size()
                    && key.equals(keyFor(
                    highlightSequence.get(highlightIndex).stringIndex,
                    highlightSequence.get(highlightIndex).fret)));

            // dibuja halo amarillo detrás si es actual
            if (isCurrentHighlight) {
                canvas.drawCircle(x, y, noteRadius + highlightHaloExtra, highlightBackgroundPaint);
            }

            // color: raíz azul, resto negro
            int fillColor = note.isRoot ? 0xFF1E88E5 : 0xFF222222;

            Paint fillPaint = new Paint();
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAntiAlias(true);
            fillPaint.setColor(fillColor);
            canvas.drawCircle(x, y, noteRadius, fillPaint);

            // root: borde blanco fino
            if (note.isRoot) {
                Paint border = new Paint();
                border.setStyle(Paint.Style.STROKE);
                border.setAntiAlias(true);
                border.setStrokeWidth(3f);
                border.setColor(0xFFFFFFFF);
                canvas.drawCircle(x, y, noteRadius + 2f, border);
            }

            // etiqueta (nombre de nota) en blanco
            canvas.drawText(note.noteName, x, y + (degreePaint.getTextSize() / 3f), degreePaint);
        }

        // 5. Números de traste abajo con separación
        int[] showFrets = {1, 3, 5, 7, 9, 12};
        float extraGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        float textY = topOffset + boardHeight + extraGap + degreePaint.getTextSize();
        for (int fret : showFrets) {
            float x = effectiveLeft + fret * fretSpacing - fretSpacing / 2;
            canvas.drawText(String.valueOf(fret), x, textY, fretNumberPaint);
        }
    }

    public static List<ScaleFretNote> generatePentatonicBox() {
        List<ScaleFretNote> notes = new ArrayList<>();
        notes.add(new ScaleFretNote(0, 0, "R", true, null, "A"));
        notes.add(new ScaleFretNote(0, 8, "b3", false, null, "C"));
        notes.add(new ScaleFretNote(1, 5, "p4", false, null, "D"));
        notes.add(new ScaleFretNote(1, 7, "p5", false, null, "E"));
        notes.add(new ScaleFretNote(2, 5, "b7", false, null, "G"));
        notes.add(new ScaleFretNote(2, 7, "R", true, null, "A"));
        notes.add(new ScaleFretNote(3, 5, "b3", false, null, "C"));
        notes.add(new ScaleFretNote(3, 7, "p4", false, null, "D"));
        notes.add(new ScaleFretNote(4, 5, "p5", false, null, "E"));
        notes.add(new ScaleFretNote(4, 8, "b7", false, null, "G"));
        notes.add(new ScaleFretNote(5, 5, "R", true, null, "A"));
        notes.add(new ScaleFretNote(5, 8, "b3", false, null, "C"));
        return notes;
    }
}

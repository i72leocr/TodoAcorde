package com.todoacorde.todoacorde.scales.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.todoacorde.todoacorde.scales.data.ScaleFretNote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Vista personalizada que dibuja un diapasón para mostrar patrones/box de escalas.
 *
 * Características principales:
 * - 6 cuerdas, 13 trastes (incluye la cejuela como traste 0).
 * - Dibuja marcadores de nota con énfasis visual en la tónica (isRoot).
 * - Permite resaltar una secuencia de notas y un índice actual con un halo.
 * - Dibuja marcadores de posición de traste (3, 5, 7, 9, 12) y números de traste.
 */
public class ScaleFretboardView extends View {

    /* Tag para logs. */
    private static final String TAG = "ScaleFretboardView";

    /* Pinceles de dibujo. */
    private final Paint stringPaint = new Paint();
    private final Paint fretPaint = new Paint();
    private final Paint nutPaint = new Paint();
    private final Paint degreePaint = new Paint();
    private final Paint fretNumberPaint = new Paint();
    private final Paint highlightBackgroundPaint = new Paint();

    /* Dimensiones del diapasón. */
    private final int numStrings = 6;
    private final int numFrets = 13; /* Incluye cejuela (0) → 12. */

    /* Estilo de nota y halo. */
    private final float noteRadius = 30f;
    private final float highlightHaloExtra = 16f;

    /* Métricas y paddings calculados. */
    private float verticalBasePaddingPx;
    private float extraBottomForFretNumbers;
    private float fretSpacing;
    private float stringSpacing;
    private float extraLeftPadding;

    /* Datos a renderizar. */
    private List<ScaleFretNote> scaleNotes = new ArrayList<>();
    private List<ScaleFretNote> highlightSequence = new ArrayList<>();
    private int highlightIndex = -1;

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

    /**
     * Inicializa pinceles y valores predeterminados.
     */
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
        fretNumberPaint.setAntiAlias(true);

        highlightBackgroundPaint.setColor(Color.argb(0xCC, 0xFF, 0xEB, 0x3B));
        highlightBackgroundPaint.setStyle(Paint.Style.FILL);
        highlightBackgroundPaint.setAntiAlias(true);

        verticalBasePaddingPx = dp(4);
        extraBottomForFretNumbers = dp(28);
        /* Margen izquierdo extra para que el halo y el texto no se recorten. */
        extraLeftPadding = noteRadius + highlightHaloExtra + dp(8);
    }

    /**
     * Establece las notas (todos los puntos del patrón) a dibujar.
     *
     * @param notes lista de notas de escala (puede ser null)
     */
    public void setScaleNotes(List<ScaleFretNote> notes) {
        this.scaleNotes = (notes != null) ? notes : new ArrayList<>();
        Log.i(TAG, "setScaleNotes: count=" + this.scaleNotes.size());
        invalidate();
    }

    /**
     * Establece la secuencia a resaltar (orden de reproducción/recorrido).
     *
     * @param sequence lista en orden; null equivale a limpiar la secuencia
     */
    public void setHighlightSequence(List<ScaleFretNote> sequence) {
        if (sequence == null) {
            clearHighlightSequence();
            return;
        }
        highlightSequence = new ArrayList<>(sequence);
        if (highlightIndex >= highlightSequence.size()) {
            highlightIndex = -1;
        }
        Log.i(TAG, "Highlight sequence set. size=" + highlightSequence.size());
        invalidate();
    }

    /**
     * Limpia la secuencia resaltada.
     */
    public void clearHighlightSequence() {
        if (highlightSequence != null) highlightSequence.clear();
        highlightIndex = -1;
        Log.i(TAG, "Highlight sequence cleared.");
        invalidate();
    }

    /**
     * Fija el índice actual dentro de la secuencia resaltada.
     *
     * @param idx índice base 0, -1 para desactivar
     */
    public void setHighlightIndex(int idx) {
        if (idx == -1) {
            highlightIndex = -1;
            invalidate();
            return;
        }
        if (highlightSequence == null || highlightSequence.isEmpty()) return;
        if (idx >= 0 && idx < highlightSequence.size()) {
            highlightIndex = idx;
            Log.i(TAG, "Highlight index set to " + idx);
            invalidate();
        } else {
            Log.w(TAG, "Invalid highlightIndex " + idx + " size=" + getHighlightSequenceSize());
        }
    }

    /**
     * @return tamaño de la secuencia resaltada (0 si no hay secuencia)
     */
    public int getHighlightSequenceSize() {
        return (highlightSequence == null) ? 0 : highlightSequence.size();
    }

    /**
     * Calcula dimensiones del control en función del ancho disponible,
     * ajustando el padding izquierdo para no recortar halo y texto.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        /* Itera para estabilizar el padding izquierdo en función del espacio de traste. */
        float basePad = noteRadius + highlightHaloExtra + dp(8);
        float elp = Math.max(basePad, extraLeftPadding);
        for (int i = 0; i < 3; i++) {
            float usableWidth = Math.max(1f, width - elp);
            float fs = usableWidth / numFrets;
            float needed = basePad + fs / 2f;
            if (needed - elp > 0.5f) {
                elp = needed;
            } else {
                break;
            }
        }
        extraLeftPadding = elp;

        float usableWidth = Math.max(1f, width - extraLeftPadding);
        fretSpacing = usableWidth / numFrets;
        stringSpacing = fretSpacing * 0.75f;

        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + dp(6));
        int height = (int) (topOffset + (numStrings - 1) * stringSpacing + extraBottomForFretNumbers);

        setMeasuredDimension(width, height);
    }

    /**
     * Dibuja cuerdas, trastes, marcadores de traste, notas y halo del índice activo.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padL = getPaddingLeft();
        float availableWidth = getWidth() - padL - getPaddingRight();
        float usableWidth = Math.max(1f, availableWidth - extraLeftPadding);
        fretSpacing = usableWidth / numFrets;

        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + dp(6));
        float boardHeight = (numStrings - 1) * stringSpacing;
        float left = padL + extraLeftPadding;

        /* Cuerdas. */
        for (int i = 0; i < numStrings; i++) {
            float y = topOffset + (numStrings - 1 - i) * stringSpacing;
            canvas.drawLine(left, y, left + numFrets * fretSpacing, y, stringPaint);
        }

        /* Trastes (incluye cejuela). */
        for (int i = 0; i <= numFrets; i++) {
            float x = left + i * fretSpacing;
            canvas.drawLine(x, topOffset, x, topOffset + boardHeight, i == 0 ? nutPaint : fretPaint);
        }

        /* Marcadores de posición típicos (3,5,7,9,12). */
        int[] markerFrets = {3, 5, 7, 9, 12};
        float markerRadius = dp(7);
        for (int f : markerFrets) {
            float x = left + f * fretSpacing - fretSpacing / 2f;
            float y = topOffset + boardHeight / 2f;
            canvas.drawCircle(x, y, markerRadius, fretPaint);
        }

        /* Notas del patrón. */
        for (ScaleFretNote note : scaleNotes) {
            int s = note.stringIndex;
            int f = note.fret;

            float y = topOffset + (numStrings - 1 - s) * stringSpacing;
            float x;
            if (f == 0) {
                /* Nota “abierta” dibujada a la izquierda de la cejuela. */
                x = left - fretSpacing / 2f;
            } else {
                x = left + f * fretSpacing - fretSpacing / 2f;
            }

            /* ¿Es la nota actualmente resaltada en la secuencia? */
            boolean isCurrent = false;
            if (highlightIndex >= 0 && highlightIndex < getHighlightSequenceSize()) {
                ScaleFretNote cur = highlightSequence.get(highlightIndex);
                isCurrent = (cur.stringIndex == s && cur.fret == f);
            }
            if (isCurrent) {
                canvas.drawCircle(x, y, noteRadius + highlightHaloExtra, highlightBackgroundPaint);
            }

            /* Círculo base (tónica en azul, resto en gris oscuro). */
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(note.isRoot ? 0xFF1E88E5 : 0xFF222222);
            canvas.drawCircle(x, y, noteRadius, fill);

            /* Borde para tónica. */
            if (note.isRoot) {
                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3f);
                border.setColor(Color.WHITE);
                canvas.drawCircle(x, y, noteRadius + 2f, border);
            }

            /* Texto: nombre de nota normalizado. */
            String displayName = toSharp(note.noteName);
            canvas.drawText(displayName, x, y + (degreePaint.getTextSize() / 3f), degreePaint);
        }

        /* Números de traste visibles. */
        int[] showFrets = {1, 3, 5, 7, 9, 12};
        float textY = topOffset + boardHeight + dp(6) + degreePaint.getTextSize();
        for (int f : showFrets) {
            float x = left + f * fretSpacing - fretSpacing / 2f;
            canvas.drawText(String.valueOf(f), x, textY, fretNumberPaint);
        }
    }

    /**
     * Conversión dp → px.
     */
    private float dp(int dps) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dps,
                getResources().getDisplayMetrics()
        );
    }

    /**
     * Normaliza nombres de nota a sostenidos.
     */
    private static String toSharp(String s) {
        if (s == null) return "";
        s = s.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "DB":
                return "C#";
            case "EB":
                return "D#";
            case "GB":
                return "F#";
            case "AB":
                return "G#";
            case "BB":
                return "A#";
            default:
                return s;
        }
    }
}

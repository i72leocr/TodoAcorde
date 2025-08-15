package com.tuguitar.todoacorde.scales.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.tuguitar.todoacorde.scales.data.ScaleFretNote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dibuja un diapasón para mostrar patrones de escala.
 * - 6 cuerdas, 13 trastes (0..12). El traste 0 se representa como una zona a la IZQUIERDA de la cejuela.
 * - Las notas raíz se pintan en azul con borde blanco
 * - Resaltado (halo) de la nota actual según una secuencia opcional (sin reordenar)
 * - Las etiquetas de nota se muestran en sostenidos (Db -> C#, etc.)
 */
public class ScaleFretboardView extends View {

    private static final String TAG = "ScaleFretboardView";

    // Pinturas
    private final Paint stringPaint = new Paint();
    private final Paint fretPaint = new Paint();
    private final Paint nutPaint = new Paint();
    private final Paint degreePaint = new Paint();       // texto dentro del punto (noteName)
    private final Paint fretNumberPaint = new Paint();
    private final Paint highlightBackgroundPaint = new Paint();

    // Geometría
    private final int numStrings = 6;
    private final int numFrets = 13; // 0..12, donde 0 es la zona izquierda de la cejuela
    private final float noteRadius = 30f;
    private final float highlightHaloExtra = 16f;
    private float verticalBasePaddingPx;
    private float extraBottomForFretNumbers;
    private float fretSpacing;
    private float stringSpacing;
    private float extraLeftPadding; // margen para que la nota al aire (0) a la izquierda no se corte

    // Datos
    private List<ScaleFretNote> scaleNotes = new ArrayList<>();

    // Resaltado
    private List<ScaleFretNote> highlightSequence = new ArrayList<>();
    private int highlightIndex = -1; // -1 = sin selección

    // ---------------------------------------------------------------------------------------------
    // Constructores
    // ---------------------------------------------------------------------------------------------
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

    // ---------------------------------------------------------------------------------------------
    // Init
    // ---------------------------------------------------------------------------------------------
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

        // Halo amarillo semiopaco para la nota resaltada
        highlightBackgroundPaint.setColor(Color.argb(0xCC, 0xFF, 0xEB, 0x3B));
        highlightBackgroundPaint.setStyle(Paint.Style.FILL);
        highlightBackgroundPaint.setAntiAlias(true);

        verticalBasePaddingPx = dp(4);
        extraBottomForFretNumbers = dp(28);

        // Valor base; lo ajustaremos en onMeasure en función de fretSpacing
        extraLeftPadding = noteRadius + highlightHaloExtra + dp(8);
    }

    // ---------------------------------------------------------------------------------------------
    // API pública
    // ---------------------------------------------------------------------------------------------

    /** Establece las notas a dibujar (lista completa del patrón). */
    public void setScaleNotes(List<ScaleFretNote> notes) {
        this.scaleNotes = (notes != null) ? notes : new ArrayList<>();
        Log.i(TAG, "setScaleNotes: count=" + this.scaleNotes.size());
        invalidate();
    }

    /** Define la secuencia de notas a resaltar (¡respeta el orden tal cual llega!). */
    public void setHighlightSequence(List<ScaleFretNote> sequence) {
        if (sequence == null) {
            clearHighlightSequence();
            return;
        }
        // NO ordenar: el orden lo decide el VM/Fragment
        highlightSequence = new ArrayList<>(sequence);

        // ❗No forzar highlightIndex=0 aquí. Si ya hay índice válido lo conservamos,
        // si no, dejamos -1 (sin selección) hasta que el fragment decida.
        if (highlightIndex >= highlightSequence.size()) {
            highlightIndex = -1;
        }

        Log.i(TAG, "Highlight sequence set. size=" + highlightSequence.size());
        invalidate();
    }

    /** Limpia la secuencia de resaltado y oculta el halo. */
    public void clearHighlightSequence() {
        if (highlightSequence != null) highlightSequence.clear();
        highlightIndex = -1;
        Log.i(TAG, "Highlight sequence cleared.");
        invalidate();
    }

    /** Cambia el índice actual de la secuencia (acepta -1 como 'sin selección'). */
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

    /** Tamaño de la secuencia de resaltado. */
    public int getHighlightSequenceSize() {
        return (highlightSequence == null) ? 0 : highlightSequence.size();
    }

    // ---------------------------------------------------------------------------------------------
    // Measure / Draw
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        // Ajuste adaptativo de padding izquierdo:
        float basePad = noteRadius + highlightHaloExtra + dp(8);
        float elp = Math.max(basePad, extraLeftPadding);
        for (int i = 0; i < 3; i++) {
            float usableWidth = Math.max(1f, width - elp);
            float fs = usableWidth / numFrets;
            float needed = basePad + fs / 2f; // margen + mitad de un traste para centrar el 0 a la izquierda
            if (needed - elp > 0.5f) {
                elp = needed;
            } else {
                break;
            }
        }
        extraLeftPadding = elp;

        float usableWidth = Math.max(1f, width - extraLeftPadding);
        fretSpacing = usableWidth / numFrets;
        stringSpacing = fretSpacing * 0.75f; // aire vertical para puntos grandes
        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + dp(6));
        int height = (int) (topOffset + (numStrings - 1) * stringSpacing + extraBottomForFretNumbers);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padL = getPaddingLeft();
        float availableWidth = getWidth() - padL - getPaddingRight();
        float usableWidth = Math.max(1f, availableWidth - extraLeftPadding);
        fretSpacing = usableWidth / numFrets;

        float topOffset = Math.max(verticalBasePaddingPx, noteRadius + dp(6));
        float boardHeight = (numStrings - 1) * stringSpacing;
        float left = padL + extraLeftPadding; // X de la cejuela

        // 1) cuerdas
        for (int i = 0; i < numStrings; i++) {
            float y = topOffset + (numStrings - 1 - i) * stringSpacing;
            canvas.drawLine(left, y, left + numFrets * fretSpacing, y, stringPaint);
        }

        // 2) trastes (0 = cejuela)
        for (int i = 0; i <= numFrets; i++) {
            float x = left + i * fretSpacing;
            canvas.drawLine(x, topOffset, x, topOffset + boardHeight, i == 0 ? nutPaint : fretPaint);
        }

        // 3) marcadores de traste
        int[] markerFrets = {3, 5, 7, 9, 12};
        float markerRadius = dp(7);
        for (int f : markerFrets) {
            float x = left + f * fretSpacing - fretSpacing / 2f;
            float y = topOffset + boardHeight / 2f;
            canvas.drawCircle(x, y, markerRadius, fretPaint);
        }

        // 4) notas del patrón
        for (ScaleFretNote note : scaleNotes) {
            int s = note.stringIndex;
            int f = note.fret;

            float y = topOffset + (numStrings - 1 - s) * stringSpacing;

            // Centrar el traste 0 a la IZQUIERDA de la cejuela
            float x;
            if (f == 0) {
                x = left - fretSpacing / 2f;
            } else {
                x = left + f * fretSpacing - fretSpacing / 2f;
            }

            // ¿esta nota es la actual de la secuencia?
            boolean isCurrent = false;
            if (highlightIndex >= 0 && highlightIndex < getHighlightSequenceSize()) {
                ScaleFretNote cur = highlightSequence.get(highlightIndex);
                isCurrent = (cur.stringIndex == s && cur.fret == f);
            }

            // halo detrás si es la nota actual
            if (isCurrent) {
                canvas.drawCircle(x, y, noteRadius + highlightHaloExtra, highlightBackgroundPaint);
            }

            // relleno (raíz azul, resto negro)
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(note.isRoot ? 0xFF1E88E5 : 0xFF222222);
            canvas.drawCircle(x, y, noteRadius, fill);

            // borde blanco fino para la raíz
            if (note.isRoot) {
                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3f);
                border.setColor(Color.WHITE);
                canvas.drawCircle(x, y, noteRadius + 2f, border);
            }

            // etiqueta: nombre de nota (en sostenidos)
            String displayName = toSharp(note.noteName);
            canvas.drawText(displayName, x, y + (degreePaint.getTextSize() / 3f), degreePaint);
        }

        // 5) números de traste
        int[] showFrets = {1, 3, 5, 7, 9, 12};
        float textY = topOffset + boardHeight + dp(6) + degreePaint.getTextSize();
        for (int f : showFrets) {
            float x = left + f * fretSpacing - fretSpacing / 2f;
            canvas.drawText(String.valueOf(f), x, textY, fretNumberPaint);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utils
    // ---------------------------------------------------------------------------------------------
    private float dp(int dps) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dps,
                getResources().getDisplayMetrics()
        );
    }

    /** Convierte posibles bemoles a su enarmónico en sostenidos para mostrar. */
    private static String toSharp(String s) {
        if (s == null) return "";
        s = s.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
            default:   return s;
        }
    }
}

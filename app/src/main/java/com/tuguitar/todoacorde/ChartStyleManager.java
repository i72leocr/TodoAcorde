package com.tuguitar.todoacorde;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class ChartStyleManager {
    /**
     * Aplica estilo a un PieChart.
     * @param chart PieChart a estilizar
     * @param data PieData con los valores
     * @param legendLabels lista de etiquetas para la leyenda (orden corresponde a los colores)
     */
    public static void stylePieChart(PieChart chart, PieData data, List<String> legendLabels) {
        chart.clear(); // limpia sin romper rendering
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);

        // 🔸 ESTILO DE LOS VALORES EN EL GRÁFICO (porcentajes)
        chart.setUsePercentValues(true); // Mostrar % en lugar de valor bruto
        chart.setDrawEntryLabels(true); // Mostrar etiquetas en el gráfico
        chart.setEntryLabelColor(Color.WHITE); // Color del texto en el gráfico
        chart.setEntryLabelTextSize(14f); // Tamaño del texto en el gráfico

        // 🔸 ESTILO DEL DATASET (tamaño y color de los valores)
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);

        // 🔸 LEYENDA
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        List<LegendEntry> entries = new ArrayList<>();

        if (legendLabels != null && !legendLabels.isEmpty()) {
            List<Integer> colors = ((PieDataSet) data.getDataSet()).getColors();
            int count = Math.min(legendLabels.size(), data.getEntryCount());
            count = Math.min(count, colors.size());

            for (int i = 0; i < count; i++) {
                LegendEntry entry = new LegendEntry();
                entry.label = legendLabels.get(i);
                entry.formColor = colors.get(i);
                entry.form = Legend.LegendForm.SQUARE;
                entries.add(entry);
            }

            legend.setCustom(entries);
        } else {
            legend.setEnabled(false);
        }

        chart.invalidate();
    }

    /**
     * Aplica estilo a un LineChart.
     * @param chart LineChart a estilizar
     * @param data LineData con los valores
     * @param xLabels lista de etiquetas para el eje X
     */
    public static void styleLineChart(LineChart chart, LineData data, List<String> xLabels) {
        chart.setData(data);
        XAxis x = chart.getXAxis();
        x.removeAllLimitLines();
        x.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        x.setGranularity(1f);
        x.setGranularityEnabled(true);
        x.setCenterAxisLabels(true);
        x.setLabelCount(xLabels.size(), true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setLabelRotationAngle(40f);
        x.setTextSize(10f);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setAvoidFirstLastClipping(false);

        LimitLine left = new LimitLine(0f, "");
        left.setLineWidth(2f);
        left.setLineColor(Color.BLACK);
        x.addLimitLine(left);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(xLabels.size() - 1f);

        chart.setExtraBottomOffset(40f);

        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(true);
        y.setDrawAxisLine(false);
        y.removeAllLimitLines();

        // --- SOLO ENTEROS EN EL EJE Y ---
        y.setGranularity(1f); // pasos de 1
        y.setGranularityEnabled(true);
        y.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Puedes calcular el máximo real, por si quieres hacer "barras" más agradables
        float maxY = 0f;
        for (ILineDataSet set : data.getDataSets()) {
            for (int i = 0; i < set.getEntryCount(); i++) {
                float v = set.getEntryForIndex(i).getY();
                if (v > maxY) maxY = v;
            }
        }
        // Si quieres que sea múltiplo de 10 o 5 para estética:
        float axisMax = (float) (Math.ceil(maxY / 10.0) * 10);
        y.setAxisMaximum(axisMax < 10 ? 10 : axisMax);

        LimitLine zeroY = new LimitLine(0f, "");
        zeroY.setLineWidth(2f);
        zeroY.setLineColor(Color.BLACK);
        y.addLimitLine(zeroY);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }

    /**
     * Aplica estilo a un BarChart.
     * @param chart BarChart a estilizar
     * @param data BarData con los valores
     * @param xLabels lista de etiquetas para el eje X (longitud = data entries + 1 si hay espacio vacío)
     * @param legendColors lista de colores para la leyenda (uno por barra)
     */
    public static void styleBarChart(BarChart chart, BarData data, List<String> xLabels, List<Integer> legendColors) {
        chart.setData(data);
        XAxis x = chart.getXAxis();
        x.removeAllLimitLines();
        x.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setLabelRotationAngle(0f);
        x.setTextSize(10f);
        x.setGranularity(1f);
        x.setGranularityEnabled(true);
        x.setLabelCount(xLabels.size(), true);
        x.setCenterAxisLabels(false);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setAvoidFirstLastClipping(false);

        // Línea negra vertical a la izquierda de la primera barra
        LimitLine left = new LimitLine(0.5f, "");
        left.setLineWidth(2f);
        left.setLineColor(Color.BLACK);
        x.addLimitLine(left);
        x.setAxisMinimum(0.5f);
        x.setAxisMaximum(xLabels.size() - 0.5f);

        chart.setExtraBottomOffset(40f);

        // === BLOQUE PARA MOSTRAR SOLO ENTEROS EN EL EJE Y ===
        float maxY = 0f;
        for (IBarDataSet set : data.getDataSets()) {
            for (int i = 0; i < set.getEntryCount(); i++) {
                float yVal = set.getEntryForIndex(i).getY();
                if (yVal > maxY) maxY = yVal;
            }
        }
        maxY = (float) Math.ceil(maxY);

        YAxis y = chart.getAxisLeft();
        y.setAxisMinimum(0f);
        y.setAxisMaximum(maxY < 5 ? 5 : maxY); // puedes quitar el "< 5 ? 5 :" si quieres ajustar justo
        y.setGranularity(1f); // Solo enteros
        y.setLabelCount((int) (maxY < 5 ? 5 : maxY) + 1, true); // +1 porque es inclusivo
        y.setDrawGridLines(true);
        y.setDrawAxisLine(false);
        y.removeAllLimitLines();
        LimitLine zeroY = new LimitLine(0f, "");
        zeroY.setLineWidth(2f);
        zeroY.setLineColor(Color.BLACK);
        y.addLimitLine(zeroY);
        y.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        // === FIN BLOQUE ===

        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        if (legendColors != null && !legendColors.isEmpty()) {
            List<LegendEntry> entries = new ArrayList<>();
            for (int i = 1; i < xLabels.size(); i++) {
                LegendEntry entry = new LegendEntry();
                entry.label = xLabels.get(i); // Saltamos el padding
                entry.formColor = legendColors.get(i - 1);
                entry.form = Legend.LegendForm.SQUARE;
                entries.add(entry);
            }
            legend.setCustom(entries);
            legend.setFormSize(14f);
            legend.setTextSize(13f);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
        } else {
            legend.setEnabled(false);
        }

        chart.invalidate();
    }

    public static void styleHorizontalBarChart(HorizontalBarChart chart, BarData data, List<String> yLabels, List<Integer> colors) {
        chart.setData(data);
        chart.setExtraLeftOffset(30f);
        chart.setExtraRightOffset(20f);
        chart.setExtraBottomOffset(20f);



        // Eje Y izquierdo (valores)
        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(false);
        left.setGranularity(1f);
        left.setTextSize(12f);

        // Eje Y derecho desactivado
        chart.getAxisRight().setEnabled(false);

        // Eje X (etiquetas de acordes)
        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(yLabels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextSize(12f);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setGranularity(1f);
        x.setLabelCount(yLabels.size());
        x.setCenterAxisLabels(false);

        // Leyenda
        Legend legend = chart.getLegend();
        legend.setEnabled(false); // opcional, ya que los acordes están a la izquierda

        // Dataset
        BarDataSet dataSet = (BarDataSet) data.getDataSetByIndex(0);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);

        chart.invalidate();
    }


}
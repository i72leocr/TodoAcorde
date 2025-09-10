/*package com.todoacorde.todoacorde;

import android.graphics.Color;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.text.SimpleDateFormat;
import java.util.*;

public class KpiProviders {

        public static class ChartPieKpiResult {
        public final PieData data;
        public final List<String> labels;
        public ChartPieKpiResult(PieData data, List<String> labels) {
            this.data = data;
            this.labels = labels;
        }
    }

        public static class ChartLineKpiResult {
        public final LineData data;
        public final List<String> labels;
        public ChartLineKpiResult(LineData data, List<String> labels) {
            this.data = data;
            this.labels = labels;
        }
    }

        public static class ChartBarKpiResult {
        public final BarData data;
        public final List<String> labels;
        public final List<Integer> colors;
        public ChartBarKpiResult(BarData data, List<String> labels, List<Integer> colors) {
            this.data = data;
            this.labels = labels;
            this.colors = colors;
        }
    }

    public interface KpiProvider<T> {
        T getKpiResult(int songId, long since);
    }

        public static class AciertosFallosProvider implements KpiProvider<ChartPieKpiResult> {
        private final PracticeDetailDao detailDao;
        public AciertosFallosProvider(PracticeDetailDao dao) {
            this.detailDao = dao;
        }
        @Override
        public ChartPieKpiResult getKpiResult(int songId, long since) {
            int correct = detailDao.countCorrectForSongSince(songId, since);
            int incorrect = detailDao.countIncorrectForSongSince(songId, since);

            List<PieEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            if (correct > 0) {
                entries.add(new PieEntry(correct, "Aciertos"));
                labels.add("Aciertos");
            }
            if (incorrect > 0) {
                entries.add(new PieEntry(incorrect, "Fallos"));
                labels.add("Fallos");
            }

            PieDataSet ds = new PieDataSet(entries, "");
            ds.setColors(ColorTemplate.MATERIAL_COLORS);
            ds.setValueTextColor(Color.WHITE);
            ds.setValueTextSize(14f);

            PieData pd = new PieData(ds);
            pd.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    float total = correct + incorrect;
                    return total == 0
                            ? "0%"
                            : String.format(Locale.getDefault(), "%d%%", Math.round(value * 100f / total));
                }
            });

            return new ChartPieKpiResult(pd, labels);
        }
    }

        public static class ProgressProvider implements KpiProvider<ChartLineKpiResult> {
        private final PracticeSessionDao sessionDao;
        public ProgressProvider(PracticeSessionDao dao) {
            this.sessionDao = dao;
        }
        @Override
        public ChartLineKpiResult getKpiResult(int songId, long since) {
            List<PracticeSession> sessions = sessionDao.getSessionsForSong(songId);
            Collections.sort(sessions, Comparator.comparingLong(s -> s.startTime));

            int maxPoints = 10;
            int start = Math.max(0, sessions.size() - maxPoints);

            List<Entry> pts = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM", Locale.getDefault());

            labels.add("");
            int maxScore = 0;
            for (int i = 0; i < Math.min(maxPoints, sessions.size()); i++) {
                PracticeSession s = sessions.get(start + i);
                pts.add(new Entry(i + 1, s.totalScore));
                labels.add(fmt.format(new Date(s.startTime)));
                if (s.totalScore > maxScore) maxScore = s.totalScore;
            }

            LineDataSet ds = new LineDataSet(pts, "");
            ds.setColor(ColorTemplate.MATERIAL_COLORS[0]);
            ds.setCircleColor(ColorTemplate.MATERIAL_COLORS[1]);
            ds.setCircleRadius(5f);
            ds.setLineWidth(2f);
            ds.setValueTextSize(10f);
            ds.setDrawValues(true);

                        ds.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPointLabel(Entry entry) {
                    return String.valueOf((int) entry.getY());
                }
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });

            ChartLineKpiResult result = new ChartLineKpiResult(new LineData(ds), labels);
            result.data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });
            return result;
        }
    }

        public static class ScoreBucketsProvider implements KpiProvider<ChartBarKpiResult> {
        private final PracticeSessionDao sessionDao;
        public ScoreBucketsProvider(PracticeSessionDao dao) {
            this.sessionDao = dao;
        }
        @Override
        public ChartBarKpiResult getKpiResult(int songId, long since) {
            List<PracticeSession> sessions = sessionDao.getSessionsForSong(songId);
            int[] buckets = new int[5];
            for (PracticeSession s : sessions) {
                int idx = Math.min(4, s.totalScore / 20);
                buckets[idx]++;
            }

            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                entries.add(new BarEntry(i + 1, buckets[i]));
            }

                        List<Integer> colors = Arrays.asList(
                    Color.parseColor("#E53935"),                     Color.parseColor("#FB8C00"),                     Color.parseColor("#FDD835"),                     Color.parseColor("#AEEA00"),                     Color.parseColor("#388E3C")              );

            BarDataSet ds = new BarDataSet(entries, "");
            ds.setColors(colors);
            ds.setValueTextColor(Color.BLACK);
            ds.setValueTextSize(12f);

                        ds.setValueFormatter(new ValueFormatter() {
                @Override
                public String getBarLabel(BarEntry barEntry) {
                    return String.valueOf((int) barEntry.getY());
                }
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });
            
            BarData bd = new BarData(ds);
            bd.setBarWidth(0.7f);

            List<String> labels = Arrays.asList("", "0–19", "20–39", "40–59", "60–79", "80–100");
            return new ChartBarKpiResult(bd, labels, colors);
        }
    }

        public static class TopErroredChordsProvider implements KpiProvider<ChartPieKpiResult> {
        private final PracticeDetailDao detailDao;
        private final ChordDao chordDao;

        public TopErroredChordsProvider(PracticeDetailDao detailDao, ChordDao chordDao) {
            this.detailDao = detailDao;
            this.chordDao = chordDao;
        }

        @Override
        public ChartPieKpiResult getKpiResult(int songId, long since) {
            List<PracticeDetailDao.ChordPercentage> results = detailDao.getTopErroredChordsByPercentage(songId, since);
            List<PieEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            int total = 0;
                        int[] gradientColors = {
                    Color.parseColor("#E53935"),                     Color.parseColor("#FB8C00"),                     Color.parseColor("#FFD600")              };

            if (results != null && !results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    PracticeDetailDao.ChordPercentage row = results.get(i);
                    Chord chord = chordDao.getChordById(row.getChordId());
                    if (chord != null) {
                        String chordName = chord.getName();
                        entries.add(new PieEntry(row.getPercentage(), chordName));
                        labels.add(chordName);
                        total += (int) row.getPercentage();
                                                colors.add(gradientColors[Math.min(i, gradientColors.length - 1)]);
                    }
                }
            }

            if (entries.isEmpty()) {
                entries.add(new PieEntry(1f, "Sin datos"));
                labels.add("Sin datos");
                PieDataSet ds = new PieDataSet(entries, "");
                ds.setColors(Color.LTGRAY);
                ds.setValueTextColor(Color.DKGRAY);
                ds.setValueTextSize(12f);

                PieData pd = new PieData(ds);
                pd.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return "";
                    }
                });

                return new ChartPieKpiResult(pd, labels);
            }

                        PieDataSet ds = new PieDataSet(entries, "");
            ds.setColors(colors);
            ds.setValueTextColor(Color.WHITE);
            ds.setValueTextSize(14f);

            PieData pd = new PieData(ds);
            final int totalFinal = total;
            pd.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return totalFinal == 0 ? "0%" :
                            String.format(Locale.getDefault(), "%d%%", Math.round(value * 100f / totalFinal));
                }
            });

            return new ChartPieKpiResult(pd, labels);
        }

    }

        public static class BestSessionKpiResult {
        public String date;
        public int correctCount;
        public int incorrectCount;
        public float correctPercentage;           public float incorrectPercentage;
        public List<String> chordNames;
        public List<Float> chordAccuracies; 
        public BestSessionKpiResult(String date,
                                    int correctCount,
                                    int incorrectCount,
                                    float correctPercentage,
                                    float incorrectPercentage,
                                    List<String> chordNames,
                                    List<Float> chordAccuracies) {
            this.date = date;
            this.correctCount = correctCount;
            this.incorrectCount = incorrectCount;
            this.correctPercentage = correctPercentage;
            this.incorrectPercentage = incorrectPercentage;
            this.chordNames = chordNames;
            this.chordAccuracies = chordAccuracies;
        }
    }

        public static class BestSessionProvider implements KpiProvider<BestSessionKpiResult> {
        private final PracticeSessionDao sessionDao;
        private final PracticeDetailDao detailDao;
        private final ChordDao chordDao;

        public BestSessionProvider(PracticeSessionDao sessionDao, PracticeDetailDao detailDao, ChordDao chordDao) {
            this.sessionDao = sessionDao;
            this.detailDao = detailDao;
            this.chordDao = chordDao;
        }

        @Override
        public BestSessionKpiResult getKpiResult(int songId, long since) {
            PracticeSession bestSession = sessionDao.getBestSessionForSong(songId, since);
            if (bestSession == null) return null;

            List<PracticeDetailDao.ChordAccuracy> accuracyList =
                    detailDao.getChordAccuraciesForSession(bestSession.id);

            int total = 0, correct = 0;
            List<String> chordNames = new ArrayList<>();
            List<Float> chordPercentages = new ArrayList<>();

            for (PracticeDetailDao.ChordAccuracy a : accuracyList) {
                int count = detailDao.getDetailsForSessionChord(bestSession.id, a.getChordId()).size();
                int correctForChord = Math.round(count * a.getPercentage() / 100f);
                total += count;
                correct += correctForChord;
                String name = chordDao.getChordById(a.getChordId()).getName();
                chordNames.add(name);
                chordPercentages.add(a.getPercentage());
            }

            int incorrect = total - correct;
            float correctPct = total > 0 ? (correct * 100f / (float) total) : 0f;
            float incorrectPct = 100f - correctPct;

            return new BestSessionKpiResult(
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(bestSession.startTime)),
                    correct,
                    incorrect,
                    correctPct,
                    incorrectPct,
                    chordNames,
                    chordPercentages
            );
        }
    }

        public static class BestSessionPieProvider implements KpiProvider<ChartPieKpiResult> {
        private final BestSessionProvider bestSessionProvider;
        public BestSessionPieProvider(BestSessionProvider bestSessionProvider) {
            this.bestSessionProvider = bestSessionProvider;
        }

        @Override
        public ChartPieKpiResult getKpiResult(int songId, long since) {
            BestSessionKpiResult best = bestSessionProvider.getKpiResult(songId, since);
            List<PieEntry> entries = new ArrayList<>();
            List<String> labels;

            int correct = (best != null) ? best.correctCount : 0;
            int incorrect = (best != null) ? best.incorrectCount : 0;
            int total = correct + incorrect;

            if (total > 0) {
                entries.add(new PieEntry(correct));
                entries.add(new PieEntry(incorrect));
                labels = Arrays.asList(correct + " aciertos", incorrect + " fallos");
            } else {
                entries.add(new PieEntry(1f));
                labels = Collections.singletonList("Sin datos");
            }

            PieDataSet ds = new PieDataSet(entries, "");
            ds.setColors(Color.GREEN, Color.RED);
            ds.setValueTextColor(Color.WHITE);
            ds.setValueTextSize(14f);

            PieData pieData = new PieData(ds);
            pieData.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (total == 0) return "";
                    return ((int) value) + "%";
                }
            });

            return new ChartPieKpiResult(pieData, labels);
        }
    }


        public static class BestSessionBarProvider implements KpiProvider<ChartBarKpiResult> {
        private final BestSessionProvider bestSessionProvider;

        public BestSessionBarProvider(BestSessionProvider bestSessionProvider) {
            this.bestSessionProvider = bestSessionProvider;
        }

        @Override
        public ChartBarKpiResult getKpiResult(int songId, long since) {
            BestSessionKpiResult best = bestSessionProvider.getKpiResult(songId, since);
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            if (best != null && !best.chordNames.isEmpty()) {
                for (int i = 0; i < best.chordNames.size(); i++) {
                    float pct = best.chordAccuracies.get(i);
                    int color;
                    if (pct < 10f) {
                        color = Color.parseColor("#E53935");                     } else if (pct < 30f) {
                        color = Color.parseColor("#FB8C00");                     } else if (pct < 50f) {
                        color = Color.parseColor("#FDD835");                     } else if (pct < 80f) {
                        color = Color.parseColor("#AEEA00");                     } else {
                        color = Color.parseColor("#388E3C");                     }
                    entries.add(new BarEntry(i, pct));
                    colors.add(color);
                    labels.add(best.chordNames.get(i));
                }

            } else {
                                entries.add(new BarEntry(0, 0));
                labels.add("Sin datos");
                colors.add(Color.LTGRAY);
            }
            BarDataSet dataSet = new BarDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(14f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawValues(true);
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getBarLabel(BarEntry barEntry) {
                                        if (labels.size() == 1 && "Sin datos".equals(labels.get(0))) return "";
                    return Math.round(barEntry.getY()) + "%";
                }
                @Override
                public String getFormattedValue(float value) {
                                        if (labels.size() == 1 && "Sin datos".equals(labels.get(0))) return "";
                    return Math.round(value) + "%";
                }
            });
            BarData barData = new BarData(dataSet);
            return new ChartBarKpiResult(barData, labels, colors);
        }
    }


        public static class TopSuccessfulChordsProvider implements KpiProvider<ChartPieKpiResult> {
        private final PracticeDetailDao detailDao;
        private final ChordDao chordDao;

        public TopSuccessfulChordsProvider(PracticeDetailDao detailDao, ChordDao chordDao) {
            this.detailDao = detailDao;
            this.chordDao = chordDao;
        }

        @Override
        public ChartPieKpiResult getKpiResult(int songId, long since) {
            List<PracticeDetailDao.ChordPercentage> results = detailDao.getTopSuccessfulChordsByPercentage(songId, since);
            List<PieEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            int total = 0;
                        int[] gradientColors = {
                    Color.parseColor("#388E3C"),                     Color.parseColor("#AEEA00"),                     Color.parseColor("#C6FF00")              };

            if (results != null && !results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    PracticeDetailDao.ChordPercentage row = results.get(i);
                    Chord chord = chordDao.getChordById(row.getChordId());
                    if (chord != null) {
                        String chordName = chord.getName();
                        entries.add(new PieEntry(row.getPercentage(), chordName));
                        labels.add(chordName);
                        total += (int) row.getPercentage();
                        colors.add(gradientColors[Math.min(i, gradientColors.length - 1)]);
                    }
                }
            }

            if (entries.isEmpty()) {
                entries.add(new PieEntry(1f, "Sin datos"));
                labels.add("Sin datos");
                PieDataSet ds = new PieDataSet(entries, "");
                ds.setColors(Color.LTGRAY);
                ds.setValueTextColor(Color.DKGRAY);
                ds.setValueTextSize(12f);

                PieData pd = new PieData(ds);
                pd.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return "";
                    }
                });

                return new ChartPieKpiResult(pd, labels);
            }

                        PieDataSet ds = new PieDataSet(entries, "");
            ds.setColors(colors);
            ds.setValueTextColor(Color.WHITE);
            ds.setValueTextSize(14f);

            PieData pd = new PieData(ds);
            final int totalFinal = total;
            pd.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return totalFinal == 0 ? "0%" :
                            String.format(Locale.getDefault(), "%d%%", Math.round(value * 100f / totalFinal));
                }
            });

            return new ChartPieKpiResult(pd, labels);
        }
    }




    }
*/
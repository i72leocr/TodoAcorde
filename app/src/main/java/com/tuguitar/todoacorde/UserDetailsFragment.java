/*package com.tuguitar.todoacorde;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.widget.Spinner;
import android.widget.LinearLayout;
import java.util.Arrays;


public class UserDetailsFragment extends Fragment {
    private TextView tvTotalSessionsCard, tvAvgScoreCard, tvMaxScoreCard, tvMinScoreCard;
    private TabLayout tabLayout;
    private View generalStatsLayout;
    private LinearLayout songStatsLayout;
    private Spinner spinnerDateFilterSong, spinnerKpi;
    private PieChart chartPie;
    private LineChart chartLine;
    private BarChart chartRange;
    private HorizontalBarChart chartBestSession;
    private PieChart chartBestSessionPie;
    private LinearLayout bestSessionLayout;

    private PracticeSessionDao sessionDao;
    private PracticeDetailDao detailDao;
    private SongDao songDao;
    private ChordDao chordDao;
    private List<Song> allSongs;
    private int selectedSongId;
    private long selectedSince;
    private TextView tvTotalSessionsGlobal, tvSongsFullCompleted;
    private LinearLayout top10ScoreTable, top10SessionsTable;
    private View logrosLayout;

    private Spinner spinnerTopStats;
    private LinearLayout top10Table;
    private RecyclerView rvAchievements;
    private AchievementAdapter achievementAdapter;
    private List<Achievement> achievements;

    private static final String[] top10Options = {
            "Top 10 canciones por Score",
            "Top 10 canciones por Sesiones"
    };



    private static final String[] dateOptions = {
            "Todo",
            "Últimos 7 días",
            "Últimos 30 días"
    };

    private static final String[] kpiOptions = {
            "Mejor sesión (desglose)",
            "Progreso últimas 10 sesiones (Fecha/Score)",
            "Franja de puntuaciones (R.Score/NºSesiones)",
            "Top 3 acordes más acertados/fallados",
            "Aciertos vs Fallos"
    };

    // KPI providers
    private KpiProviders.AciertosFallosProvider pieProv;
    private KpiProviders.ProgressProvider lineProv;
    private KpiProviders.ScoreBucketsProvider barProv;
    private KpiProviders.TopErroredChordsProvider failedChordProv;

    private KpiProviders.TopSuccessfulChordsProvider successChordProv;
    private KpiProviders.BestSessionProvider bestSessionProvider;
    private KpiProviders.BestSessionPieProvider bestSessionPieProvider;
    // en el fragment
    private KpiProviders.BestSessionBarProvider bestSessionBarProvider;

    private LinearLayout topChordsLayout;
    private PieChart chartTopSuccess, chartTopFailed;
    private Spinner spinnerSong;

    // Canciones completadas

    // Acordes por dificultad y tipo

    // Contador resumen de logros
    private TextView tvLogrosResumen;
    // En la declaración de la clase Fragment:
    private TextView tvScoreTotalCard;

    // Declaraciones agrupadas por tipo

    // TextViews
    TextView
            tvValueAchievementRepertorio, tvValueAchievementEasySongs,
            tvValueAchievementMediumSongs, tvValueAchievementHardSongs,
            tvValueAchievementUniqueChords,
            tvValueAchievementChordsEasy, tvValueAchievementChordsMedium,
            tvValueAchievementChordsHard, tvValueAchievementChordsMajor,
            tvValueAchievementChordsMinor;

    // MaterialCardViews
    MaterialCardView
            cardAchievementRepertorio, cardAchievementEasySongs,
            cardAchievementMediumSongs, cardAchievementHardSongs,
            cardAchievementUniqueChords,
            cardAchievementChordsEasy, cardAchievementChordsMedium,
            cardAchievementChordsHard, cardAchievementChordsMajor,
            cardAchievementChordsMinor;

    // ConstraintLayouts (headers)
    ConstraintLayout
            headerAchievementRepertorio, headerAchievementEasySongs,
            headerAchievementMediumSongs, headerAchievementHardSongs,
            headerAchievementUniqueChords,
            headerAchievementChordsEasy, headerAchievementChordsMedium,
            headerAchievementChordsHard, headerAchievementChordsMajor,
            headerAchievementChordsMinor;

    // ProgressBars
    ProgressBar
            progAchievementRepertorio, progAchievementEasySongs,
            progAchievementMediumSongs, progAchievementHardSongs,
            progAchievementUniqueChords,
            progAchievementChordsEasy, progAchievementChordsMedium,
            progAchievementChordsHard, progAchievementChordsMajor,
            progAchievementChordsMinor;

    // ImageViews (toggles)
    ImageView
            ivToggleAchievementRepertorio, ivToggleAchievementEasySongs,
            ivToggleAchievementMediumSongs, ivToggleAchievementHardSongs,
            ivToggleAchievementUniqueChords,
            ivToggleAchievementChordsEasy, ivToggleAchievementChordsMedium,
            ivToggleAchievementChordsHard, ivToggleAchievementChordsMajor,
            ivToggleAchievementChordsMinor;

    // LinearLayouts (sub‑achievements)
    LinearLayout
            layoutSubAchievementsRepertorio, layoutSubAchievementsEasySongs,
            layoutSubAchievementsMediumSongs, layoutSubAchievementsHardSongs,
            layoutSubAchievementsUniqueChords,
            layoutSubAchievementsChordsEasy, layoutSubAchievementsChordsMedium,
            layoutSubAchievementsChordsHard, layoutSubAchievementsChordsMajor,
            layoutSubAchievementsChordsMinor;




// ...en el onCreateView después de bestSessionProvider:



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_details, container, false);

        // Bind views
        tabLayout             = v.findViewById(R.id.tabLayout);
        generalStatsLayout    = v.findViewById(R.id.generalStatsLayout);
        songStatsLayout       = v.findViewById(R.id.songStatsLayout);
        tvTotalSessionsCard   = v.findViewById(R.id.tvTotalSessionsCard);
        tvAvgScoreCard        = v.findViewById(R.id.tvAvgScoreCard);
        tvMaxScoreCard        = v.findViewById(R.id.tvMaxScoreCard);
        tvMinScoreCard        = v.findViewById(R.id.tvMinScoreCard);
        spinnerSong           = v.findViewById(R.id.spinnerSong);
        spinnerDateFilterSong = v.findViewById(R.id.spinnerDateFilterSong);
        spinnerKpi            = v.findViewById(R.id.spinnerKpi);
        chartPie              = v.findViewById(R.id.chartPie);
        chartTopSuccess       = v.findViewById(R.id.chartTopSuccess);
        chartTopFailed        = v.findViewById(R.id.chartTopFailed);
        chartLine             = v.findViewById(R.id.chartLine);
        chartRange            = v.findViewById(R.id.chartRange);
        chartBestSession      = v.findViewById(R.id.chartBestSession);
        chartBestSessionPie   = v.findViewById(R.id.chartBestSessionPie);
        bestSessionLayout     = v.findViewById(R.id.bestSessionLayout);
        topChordsLayout   = v.findViewById(R.id.topChordsLayout);

        tvTotalSessionsGlobal   = v.findViewById(R.id.tvTotalSessionsGlobal);

        // Bind para Top10
        spinnerTopStats = v.findViewById(R.id.spinnerTopStats);
        top10Table    = v.findViewById(R.id.top10Table);
        tvScoreTotalCard = v.findViewById(R.id.tvScoreTotalCard);
        setupTop10Spinner();

        // --- CONFIGURA LAS PESTAÑAS ---
        TabLayout tabLayout = v.findViewById(R.id.tabLayout);
        View generalStats = v.findViewById(R.id.generalStatsLayout);
        View songStats    = v.findViewById(R.id.songStatsLayout);
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Generales"), true);
        tabLayout.addTab(tabLayout.newTab().setText("Por canción"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int p = tab.getPosition();
                generalStats.setVisibility(p == 0 ? View.VISIBLE : View.GONE);
                songStats   .setVisibility(p == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });



        // Disable chart interactions
        chartPie.getDescription().setEnabled(false);
        chartPie.setTouchEnabled(false);
        chartLine.getDescription().setEnabled(false);
        chartLine.setTouchEnabled(false);
        chartLine.setScaleEnabled(false);
        chartLine.setPinchZoom(false);
        chartRange.getDescription().setEnabled(false);
        chartRange.setTouchEnabled(false);
        chartRange.setScaleEnabled(false);
        chartRange.setPinchZoom(false);
        chartBestSession.getDescription().setEnabled(false);
        chartBestSession.setTouchEnabled(false);
        chartBestSession.setScaleEnabled(false);
        chartBestSession.setPinchZoom(false);
        chartBestSessionPie.getDescription().setEnabled(false);
        chartBestSessionPie.setTouchEnabled(false);

        // DAOs
        sessionDao = todoAcordeDatabase.getInstance(requireContext()).practiceSessionDao();
        detailDao  = todoAcordeDatabase.getInstance(requireContext()).practiceDetailDao();
        songDao    = todoAcordeDatabase.getInstance(requireContext()).songDao();
        chordDao   = todoAcordeDatabase.getInstance(requireContext()).chordDao();

        // State init
        allSongs = new ArrayList<>();
        selectedSongId = 1;
        selectedSince = 0L;

        // KPI providers
        pieProv         = new KpiProviders.AciertosFallosProvider(detailDao);
        lineProv        = new KpiProviders.ProgressProvider(sessionDao);
        barProv         = new KpiProviders.ScoreBucketsProvider(sessionDao);
        failedChordProv = new KpiProviders.TopErroredChordsProvider(detailDao, chordDao);
        successChordProv= new KpiProviders.TopSuccessfulChordsProvider(detailDao, chordDao);
        bestSessionProvider = new KpiProviders.BestSessionProvider(sessionDao, detailDao, chordDao);
        bestSessionPieProvider = new KpiProviders.BestSessionPieProvider(bestSessionProvider);
        bestSessionBarProvider = new KpiProviders.BestSessionBarProvider(bestSessionProvider);

        setupTabs();
        setupSongSpinner();
        setupDateFilterSpinner();
        setupKpiSpinner();
        refreshCards();
        refreshGlobalStats();



        return v;
    }

    private void setupTop10Spinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList(top10Options)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopStats.setAdapter(adapter);
        spinnerTopStats.setSelection(0);
        spinnerTopStats.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                refreshGlobalStats();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Generales"), true);
        tabLayout.addTab(tabLayout.newTab().setText("Por canción"));
        tabLayout.addTab(tabLayout.newTab().setText("Logros"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                generalStatsLayout.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                songStatsLayout.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                if (pos == 1) refreshCharts();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private int countByDifficulty(List<Song> songs, int difficultyId) {
        int count = 0;
        for (Song s : songs) {
            if (s.getDifficulty() == difficultyId) count++;
        }
        return count;
    }

    private Chord findChordById(int id, List<Chord> chords) {
        for (Chord c : chords) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    private void setupSongSpinner() {
        new Thread(() -> {
            allSongs = songDao.getAll();
            List<String> titles = new ArrayList<>();
            for (Song s : allSongs) titles.add(s.getTitle());
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        R.layout.spinner_item_highlight,
                        titles
                );
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_compact);

                spinnerSong.setAdapter(adapter);
                spinnerSong.setSelection(0);

                // Limita popup: SOLO FUNCIONA CON AppCompatSpinner (recomendado)
                spinnerSong.setDropDownWidth(dpToPx(220));          // Ancho del popup
                spinnerSong.setDropDownVerticalOffset(dpToPx(4));   // Desplazamiento vertical (+ abajo)
                spinnerSong.setDropDownHorizontalOffset(dpToPx(-20)); // Desplazamiento horizontal (+ derecha)


                if (!allSongs.isEmpty()) selectedSongId = allSongs.get(0).getId();
                spinnerSong.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        selectedSongId = allSongs.get(pos).getId();
                        refreshAll();
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            });
        }).start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }


    private void setupDateFilterSpinner() {
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, Arrays.asList(dateOptions));
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDateFilterSong.setAdapter(ad);
        spinnerDateFilterSong.setSelection(1);
        spinnerDateFilterSong.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                long now = System.currentTimeMillis();
                switch (dateOptions[pos]) {
                    case "Últimos 7 días":
                        selectedSince = now - 7L * 86_400_000;
                        break;
                    case "Últimos 30 días":
                        selectedSince = now - 30L * 86_400_000;
                        break;
                    default:
                        selectedSince = 0L;
                }
                refreshAll();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupKpiSpinner() {
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, Arrays.asList(kpiOptions));
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKpi.setAdapter(ad);
        spinnerKpi.setSelection(0);
        spinnerKpi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                refreshAll();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshAll() {
        refreshCards();
        refreshCharts();
    }
    private void refreshGlobalStats() {
        new Thread(() -> {
            // 1) Fetch datos
            List<Song> allSongs       = songDao.getAll();
            List<PracticeSession> allSessions = sessionDao.getAll();

            // 2) Agrupar sesiones por canción
            Map<Integer, List<PracticeSession>> sessionsBySong = new HashMap<>();
            for (PracticeSession s : allSessions) {
                sessionsBySong
                        .computeIfAbsent(s.songId, k -> new ArrayList<>())
                        .add(s);
            }

            // 3) Preparar Top‑10 y Score total
            List<Pair<String, Integer>> scoreAverages = new ArrayList<>();
            List<Pair<String, Integer>> sessionCounts  = new ArrayList<>();
            int totalSessions = 0;

            // Nuevo: sumar el **mejor** score de cada canción
            int sumBestScores = 0;

            for (Song song : allSongs) {
                List<PracticeSession> ss = sessionsBySong
                        .getOrDefault(song.getId(), Collections.emptyList());

                // Contar sesiones y media
                int count = ss.size(), sumThis=0, bestThis=0;
                for (PracticeSession s : ss) {
                    totalSessions++;
                    sumThis += s.totalScore;
                    bestThis = Math.max(bestThis, s.totalScore);
                }
                // Acumular mejor score para el KPI
                sumBestScores += bestThis;

                if (count > 0) {
                    scoreAverages.add(new Pair<>(song.getTitle(), sumThis / count));
                    sessionCounts .add(new Pair<>(song.getTitle(), count));
                }
            }

            // Top‑10
            scoreAverages.sort((a,b)->b.second-a.second);
            sessionCounts .sort((a,b)->b.second-a.second);
            List<Pair<String,Integer>> topScores   =
                    scoreAverages.subList(0, Math.min(10, scoreAverages.size()));
            List<Pair<String,Integer>> topSessions =
                    sessionCounts .subList(0, Math.min(10, sessionCounts.size()));

            // Máximo posible: # canciones × 100
            int totalSongs       = allSongs.size();
            final int fTotalSessions = totalSessions;
            final int fSumBestScores = sumBestScores;
            final int fMaxTotalScore = totalSongs * 100;
            final List<Pair<String,Integer>> fTopScores   = topScores;
            final List<Pair<String,Integer>> fTopSessions = topSessions;

            // 4) Update UI
            requireActivity().runOnUiThread(() -> {
                tvTotalSessionsGlobal.setText(String.valueOf(fTotalSessions));

                // **Score total** corregido
                tvScoreTotalCard.setText(fSumBestScores + " / " + fMaxTotalScore);

                // Top‑10 único
                top10Table.removeAllViews();
                LayoutInflater inf = LayoutInflater.from(getContext());
                boolean byScore = spinnerTopStats.getSelectedItemPosition()==0;
                List<Pair<String,Integer>> lista = byScore ? fTopScores : fTopSessions;

                for (int i = 0; i < lista.size(); i++) {
                    Pair<String,Integer> p = lista.get(i);
                    View row = inf.inflate(R.layout.item_top10_stat, top10Table, false);

                    TextView tvPos   = row.findViewById(R.id.tvPosition);
                    TextView tvTitle = row.findViewById(R.id.tvSongTitle);
                    TextView tvVal   = row.findViewById(R.id.tvStatValue);

                    tvPos.setText((i+1) + ".");
                    tvTitle.setText(p.first);  // muestra el nombre completo
                    String txt = byScore ? (p.second + "%") : (p.second + " sesiones");
                    tvVal.setText(txt);

                    top10Table.addView(row);
                }
            });
        }).start();
    }






    private void refreshCards() {
        new Thread(() -> {
            List<PracticeSession> ss = sessionDao.getSessionsForSong(selectedSongId);
            int total = ss.size(), sum = 0, max = 0, min = total > 0 ? ss.get(0).totalScore : 0;
            for (PracticeSession s : ss) {
                sum += s.totalScore;
                max = Math.max(max, s.totalScore);
                min = Math.min(min, s.totalScore);
            }
            int avg = total > 0 ? sum / total : 0;
            final int fTot = total, fAvg = avg, fMax = max, fMin = min;
            requireActivity().runOnUiThread(() -> {
                tvTotalSessionsCard.setText(String.valueOf(fTot));
                tvAvgScoreCard.setText(fAvg + "%");
                tvMaxScoreCard.setText(fMax + "%");
                tvMinScoreCard.setText(fMin + "%");
            });
        }).start();
    }

    private void refreshCharts() {
        new Thread(() -> {
            KpiProviders.ChartPieKpiResult  pie    = pieProv.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartLineKpiResult line   = lineProv.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartBarKpiResult  bar    = barProv.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartPieKpiResult  failed = failedChordProv.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartPieKpiResult  success = successChordProv.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.BestSessionKpiResult best = bestSessionProvider.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartPieKpiResult bestPie = bestSessionPieProvider.getKpiResult(selectedSongId, selectedSince);
            KpiProviders.ChartBarKpiResult bestBar = bestSessionBarProvider.getKpiResult(selectedSongId, selectedSince);

            requireActivity().runOnUiThread(() -> {
                // Oculta todos
                chartPie.setVisibility(View.GONE);
                chartLine.setVisibility(View.GONE);
                chartRange.setVisibility(View.GONE);
                chartBestSession.setVisibility(View.GONE);
                chartBestSessionPie.setVisibility(View.GONE);
                bestSessionLayout.setVisibility(View.GONE);
                chartBestSession.setVisibility(View.GONE);
                chartTopSuccess.setVisibility(View.GONE);
                chartTopFailed.setVisibility(View.GONE);
                topChordsLayout.setVisibility(View.GONE);

                switch (spinnerKpi.getSelectedItemPosition()) {
                    case 4:
                        ChartStyleManager.stylePieChart(chartPie, pie.data, pie.labels);
                        chartPie.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        ChartStyleManager.styleLineChart(chartLine, line.data, line.labels);
                        chartLine.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        chartRange.setFitBars(true);
                        ChartStyleManager.styleBarChart(chartRange, bar.data, bar.labels, bar.colors);
                        chartRange.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        ChartStyleManager.stylePieChart(chartTopFailed, failed.data, failed.labels);
                        ChartStyleManager.stylePieChart(chartTopSuccess, success.data, success.labels);

                        chartTopSuccess.setVisibility(View.VISIBLE);
                        chartTopFailed.setVisibility(View.VISIBLE);
                        topChordsLayout.setVisibility(View.VISIBLE);
                        break;
                    case 0:
                        if (bestBar != null && bestBar.labels.size() > 0 && bestBar.data.getDataSetCount() > 0) {
                            // Usa el resultado del provider, que ya tiene todo el formato, labels y colores

                            Log.d("BAR_DEBUG", "labels=" + bestBar.labels + ", entries=" + bestBar.data.getDataSetByIndex(0).getEntryCount());

                            ChartStyleManager.styleHorizontalBarChart(chartBestSession, bestBar.data, bestBar.labels, bestBar.colors);
                            chartBestSession.setVisibility(View.VISIBLE);

                            // PieChart: aciertos/fallos de la mejor sesión
                            //ChartStyleManager.stylePieChart(chartBestSessionPie, bestPie.data, bestPie.labels);
                            //  chartBestSessionPie.setVisibility(View.VISIBLE);

                            bestSessionLayout.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            });
        }).start();
    }
}*/

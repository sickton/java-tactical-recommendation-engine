package com.sickton.jgaffer.research;

import smile.feature.extraction.PCA;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;   // used by Arrays.sort in printLoadings
import java.util.List;

/**
 * PCA Research — Premier League &amp; Serie A 2024/25
 *
 * Standalone research class. Run via main() directly from IntelliJ.
 * NOT part of the Spring application — no @Component, no @Service.
 *
 * Goal: derive data-driven (attack, defence, control) weights for all 20 teams
 *       per league from real per-90 statistics using Principal Component Analysis.
 *
 * Pipeline (run once per league via runLeague()):
 *   Step 1  loadTeamNames() / loadFeatures()  — parse CSV
 *   Step 2  standardise()                     — z-score each column
 *   Step 3  PCA.fit()                         — SMILE fits the model
 *   Step 4a printVarianceExplained()          — scree / elbow
 *   Step 4b printLoadings()                   — feature → PC mapping
 *   Step 5  printTeamWeights()                — PC scores → weights
 */
public class PCAResearch {

    // ── CSV locations ─────────────────────────────────────────────────────────
    // Relative to the JGaffer/ module root (where pom.xml lives).
    // IntelliJ sets the working directory to the module root by default.
    static final String PL_CSV_PATH = "JGaffer/src/main/resources/research/PremierLeague_PCA.csv";
    static final String SA_CSV_PATH = "JGaffer/src/main/resources/research/SerieA_PCA.csv";

    // ── Feature selection ─────────────────────────────────────────────────────
    // CSV column indices (0-based). Column 0 is team_name and is handled
    // separately by loadTeamNames(); loadFeatures() reads these numeric columns.
    //
    // Full CSV column order:
    //   0  team_name
    //   1  age                        <- excluded (not a playing-style signal)
    //   2  possession
    //   3  goals_against_90
    //   4  shots_on_target_against_90
    //   5  shots
    //   6  goals_per_shot
    //   7  minutes_per_start          <- excluded (rotation metric)
    //   8  substitutions              <- excluded (rotation metric)
    //   9  unused_substitution        <- excluded (rotation metric)
    //  10  impact
    //  11  fouls
    //  12  fouls_drawn
    //  13  offside
    //  14  crosses
    //  15  interceptions
    //  16  tackles_won
    static final int[] FEATURE_COLS = { 2, 3, 4, 5, 6, 10, 11, 12, 13, 14, 15, 16 };

    // Must match FEATURE_COLS in the same order.
    static final String[] FEATURE_NAMES = {
        "possession",                   // control signal
        "goals_against_90",             // defensive signal  (lower = better defence)
        "shots_on_target_against_90",   // defensive signal  (lower = better defence)
        "shots",                        // attacking signal
        "goals_per_shot",               // attacking efficiency
        "impact",                       // overall quality index
        "fouls",                        // aggression / disruption
        "fouls_drawn",                  // attacking presence (drawing fouls in danger areas)
        "offside",                      // forward runs — attacking signal
        "crosses",                      // wide / attacking play
        "interceptions",                // defensive signal
        "tackles_won"                   // defensive signal
    };

    // Number of principal components to retain and inspect.
    static final int NUM_COMPONENTS = 3;

    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("════════════════════════════════════════");
        System.out.println("  PREMIER LEAGUE 2024/25 PCA");
        System.out.println("════════════════════════════════════════");
        runLeague(PL_CSV_PATH);

        System.out.println("════════════════════════════════════════");
        System.out.println("  SERIE A 2024/25 PCA");
        System.out.println("════════════════════════════════════════");
        runLeague(SA_CSV_PATH);
    }

    // -------------------------------------------------------------------------
    // RUN LEAGUE — executes the full PCA pipeline for one league's CSV
    // -------------------------------------------------------------------------

    static void runLeague(String csvPath) throws Exception {

        // Step 1 — load
        String[]   teamNames = loadTeamNames(csvPath);
        double[][] raw       = loadFeatures(csvPath, FEATURE_COLS);
        System.out.printf("Loaded: %d teams x %d features%n%n", raw.length, raw[0].length);

        // Step 2 — standardise
        double[][] Z = standardise(raw);

        // Step 3 — fit PCA via SMILE
        PCA pca = PCA.fit(Z);
        PCA projection = pca.getProjection(NUM_COMPONENTS);  // SMILE 3.x: returns PCA (a Transform)

        // Step 4a — variance explained
        printVarianceExplained(pca);

        // Step 4b — component loadings
        printLoadings(pca);

        // Step 5 — derive and print team weights
        // SMILE 3.x: projection.apply(double[][]) transforms the full dataset at once
        double[][] scores = projection.apply(Z);
        printTeamWeights(teamNames, scores);
    }

    // -------------------------------------------------------------------------
    // STEP 1 — CSV loading  [PROVIDED — no changes needed]
    // -------------------------------------------------------------------------

    /** Returns the 20 team names in CSV row order. */
    static String[] loadTeamNames(String path) throws Exception {
        List<String> names = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null)
                names.add(line.split(",")[0].trim());
        }
        return names.toArray(new String[0]);
    }

    /**
     * Returns a double[20][12] raw feature matrix.
     * @param cols CSV column indices to extract (must not include col 0 = team_name)
     */
    static double[][] loadFeatures(String path, int[] cols) throws Exception {
        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                double[] row   = new double[cols.length];
                for (int i = 0; i < cols.length; i++)
                    row[i] = Double.parseDouble(parts[cols[i]].trim());
                rows.add(row);
            }
        }
        return rows.toArray(new double[0][]);
    }

    // -------------------------------------------------------------------------
    // STEP 2 — Standardise  [YOUR TURN]
    // -------------------------------------------------------------------------

    /**
     * Z-score standardise every column so each feature has mean = 0, stddev = 1.
     *
     * Why: possession ranges ~40-61, goals_per_shot ~0.07-0.14.
     * Without this, large-scale features dominate the covariance matrix.
     *
     * Formula for each column j:
     *   mean_j    = (1/n) x sum of raw[i][j]  for all i
     *   stddev_j  = sqrt( (1/n) x sum of (raw[i][j] - mean_j)^2 )
     *   Z[i][j]   = (raw[i][j] - mean_j) / stddev_j
     *
     * @param raw  double[20][12]  original feature values
     * @return     double[20][12]  z-scored matrix (new array, raw unchanged)
     */
    static double[][] standardise(double[][] raw) {
        int n = raw.length;      // 20 teams
        int p = raw[0].length;   // 12 features
        double[][] Z = new double[n][p];

        for (int j = 0; j < p; j++) {
            // 1. Mean
            double mean = 0;
            for (int i = 0; i < n; i++) mean += raw[i][j];
            mean /= n;

            // 2. Population stddev
            double var = 0;
            for (int i = 0; i < n; i++) {
                double d = raw[i][j] - mean;
                var += d * d;
            }
            double stddev = Math.sqrt(var / n);

            // 3. Z-score
            for (int i = 0; i < n; i++)
                Z[i][j] = (raw[i][j] - mean) / stddev;
        }

        return Z;
    }

    // -------------------------------------------------------------------------
    // STEP 4a — Variance explained  [YOUR TURN]
    // -------------------------------------------------------------------------

    /**
     * Print how much of the total dataset variance each PC captures.
     * Use this to decide how many components are worth keeping (look for the elbow).
     *
     * SMILE hint:
     *   double[] varProp = pca.getVarianceProportion();
     *   varProp[k] is the fraction (0.0-1.0) explained by PC(k+1).
     *   Multiply by 100 for percentages.
     *
     * Target output:
     *   PC1:  38.2%   cumulative:  38.2%
     *   PC2:  21.5%   cumulative:  59.7%
     *   PC3:  14.1%   cumulative:  73.8%
     *   ...
     */
    static void printVarianceExplained(PCA pca) {
        System.out.println("=== Variance Explained per Component ===");

        double[] varProp = pca.varianceProportion();  // smile.tensor.Vector
        double cumulative = 0;
        for (int k = 0; k < varProp.length; k++) {
            double pct = varProp[k] * 100;
            cumulative += pct;
            System.out.printf("PC%-2d  %5.1f%%   cumulative: %5.1f%%%n",
                    k + 1, pct, cumulative);
        }

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // STEP 4b — Loadings  [YOUR TURN]
    // -------------------------------------------------------------------------

    /**
     * Print the loading of every original feature on each principal component.
     *
     * A loading is the correlation between an original feature and a PC.
     *   Large positive  -> feature and PC point in the same direction.
     *   Large negative  -> feature pulls against the PC direction.
     *   Near zero       -> feature has little influence on this PC.
     *
     * This output tells you what each PC *means*. If PC1 has high positive
     * loadings on shots/fouls_drawn/offside and high negative on goals_against_90,
     * PC1 represents "attacking dominance".
     *
     * SMILE hint:
     *   var loadings = pca.getLoadings();     // smile.math.matrix.Matrix
     *   loadings.get(featureIndex, componentIndex)   // 0-based for both
     *   Dimensions: [numFeatures x numComponents]
     *
     * Target output:
     *   Feature                              PC1       PC2       PC3
     *   shots                             +0.421    -0.112    +0.033
     *   fouls_drawn                       +0.389    +0.055    -0.201
     *   goals_against_90                  -0.362    +0.177    +0.128
     *   ...
     */
    static void printLoadings(PCA pca) {
        System.out.println("=== Component Loadings ===");
        System.out.printf("%-35s  %8s  %8s  %8s%n", "Feature", "PC1", "PC2", "PC3");

        var loadings = pca.loadings();  // SMILE 3.x: method returning DenseMatrix
        int n = FEATURE_NAMES.length;

        // Sort features by |PC1 loading| descending so the most influential show first
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) ->
                Double.compare(Math.abs(loadings.get(b, 0)), Math.abs(loadings.get(a, 0))));

        for (int idx : order) {
            System.out.printf("%-35s  %+8.3f  %+8.3f  %+8.3f%n",
                    FEATURE_NAMES[idx],
                    loadings.get(idx, 0),
                    loadings.get(idx, 1),
                    loadings.get(idx, 2));
        }

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // STEP 5 — Team weights  [YOUR TURN]
    // -------------------------------------------------------------------------

    /**
     * Map each team's PC scores to a normalised (attack, defence, control) triplet.
     *
     * @param teamNames  String[20]    team names in CSV row order
     * @param scores     double[20][3] row = team, col = PC score from pca.project()
     *
     * Tasks:
     *
     *   a) INTERPRET the loadings (Step 4b) to assign each PC to a dimension.
     *      The PC whose features align most with attacking stats = attack dimension.
     *      Example (yours will depend on the actual output):
     *        scores[team][0]  ->  attack
     *        scores[team][1]  ->  defence
     *        scores[team][2]  ->  control
     *
     *   b) MIN-MAX NORMALISE each chosen column to [0, 1]:
     *        normalised = (score - colMin) / (colMax - colMin)
     *      Note: if a PC loads negatively on a dimension's features, flip it:
     *        use (colMax - score) / (colMax - colMin)  so high score = stronger
     *
     *   c) ENSURE WEIGHTS SUM TO 1.0 per team:
     *        weight_attack  = norm_attack  / (norm_attack + norm_defence + norm_control)
     *        weight_defence = norm_defence / (same sum)
     *        weight_control = norm_control / (same sum)
     *
     *   d) PRINT and VALIDATE against JGaffer's current hardcoded style values:
     *
     *      Team              Current JGaffer style   Expected from data (roughly)
     *      -----------------------------------------------------------------------
     *      Man City          CONTROLLING             control weight should be highest
     *      Liverpool         CONTROLLING             control weight should be highest
     *      Arsenal           ATTACKING               attack weight should be highest
     *      Nottm Forest      DEFENSIVE               defence weight should be highest
     *      Everton           DEFENSIVE               defence weight should be highest
     *
     *      If derived weights broadly agree -> PCA is capturing real signal.
     *      If they disagree -> revisit which PC you assigned to which dimension.
     */
    static void printTeamWeights(String[] teamNames, double[][] scores) {

        // ── PC → dimension mapping ────────────────────────────────────────────
        // Read the loadings table printed above, then set these three integers
        // to the column index (0, 1, or 2) of the PC that best represents each
        // tactical dimension.
        //
        // Confirmed interpretation from PL 2024/25 loadings output:
        //
        //   PC1 (col 0): ATTACK — but all attack indicators (impact, shots, possession)
        //                load NEGATIVELY on PC1, so good/attacking teams have LOW PC1
        //                scores. flipAtk=true reverses this so high attack weight is
        //                assigned to teams with low PC1 (Arsenal, Liverpool, Man City).
        //
        //   PC2 (col 1): DEFENCE — all defensive work indicators (tackles_won −0.489,
        //                interceptions −0.473, fouls −0.355) load NEGATIVELY on PC2,
        //                so hard-working defensive teams have LOW PC2 scores.
        //                flipDef=true reverses this so Nottm Forest / Everton get high
        //                defence weights.
        //
        //   PC3 (col 2): CONTROL — possession +0.436, offside −0.570 (disciplined teams
        //                rarely get caught offside). High PC3 → more possession-based.
        //                Already correct: Liverpool 0.635, Brighton 0.533. No flip.
        int     atkCol  = 0;  boolean flipAtk  = true;
        int     defCol  = 1;  boolean flipDef  = true;
        int     ctrlCol = 2;  boolean flipCtrl = false;

        int n = scores.length;
        double[] atk  = new double[n];
        double[] def  = new double[n];
        double[] ctrl = new double[n];

        for (int i = 0; i < n; i++) {
            atk[i]  = scores[i][atkCol];
            def[i]  = scores[i][defCol];
            ctrl[i] = scores[i][ctrlCol];
        }

        // Min-max normalise each dimension to [0, 1], flipping direction if needed
        atk  = minMaxNorm(atk,  flipAtk);
        def  = minMaxNorm(def,  flipDef);
        ctrl = minMaxNorm(ctrl, flipCtrl);

        System.out.println("=== Derived Team Weights (attack / defence / control) ===");
        System.out.printf("%-25s  %8s  %8s  %8s%n", "Team", "Attack", "Defence", "Control");

        for (int i = 0; i < n; i++) {
            double sum = atk[i] + def[i] + ctrl[i];
            if (sum == 0) sum = 1;   // guard: all three PCs collapsed to min
            double wAtk  = atk[i]  / sum;
            double wDef  = def[i]  / sum;
            double wCtrl = ctrl[i] / sum;
            System.out.printf("%-25s  %8.3f  %8.3f  %8.3f%n",
                    teamNames[i], wAtk, wDef, wCtrl);
        }

        System.out.println();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Min-max normalise a column vector to [0, 1].
     * If {@code flip} is true the direction is reversed so that a high raw score
     * maps to a LOW normalised value (use when the PC loads negatively on the
     * dimension's key features).
     */
    private static double[] minMaxNorm(double[] v, boolean flip) {
        double min = v[0], max = v[0];
        for (double x : v) {
            if (x < min) min = x;
            if (x > max) max = x;
        }
        double range = max - min;
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = range == 0 ? 0.5
                    : (flip ? (max - v[i]) : (v[i] - min)) / range;
        }
        return out;
    }
}

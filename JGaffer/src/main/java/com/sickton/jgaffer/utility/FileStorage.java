package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.Formation;
import com.sickton.jgaffer.domain.Squad;
import com.sickton.jgaffer.domain.StaminaLevel;
import com.sickton.jgaffer.domain.TeamAdaptability;

/**
 * Intermediate data holder for squad information parsed from CSV.
 *
 * <p>Bundles a {@link Squad}, {@link TeamAdaptability}, and {@link StaminaLevel}
 * together as they are read from the SquadInformation CSV file. Used by
 * {@link ApplicationParser} and {@link com.sickton.jgaffer.demoUI.PremierLeagueFactory}
 * to construct {@link com.sickton.jgaffer.domain.Team} objects.</p>
 *
 * @see ApplicationParser#parseSquadInformation()
 * @author sickton
 */
public class FileStorage {
    private final Squad squadData;
    private final TeamAdaptability adaptabilityData;
    private final StaminaLevel staminaData;
    private final Formation formation;

    /**
     * PCA-derived intent weights. −1.0 ({@link #NO_WEIGHT}) means "not set";
     * the engine falls back to the style-bias formula in that case.
     * Order: attack, defence, control — matches
     * {@link com.sickton.jgaffer.domain.TeamIntent#TeamIntent(double, double, double)}.
     */
    private final double atkWeight;
    private final double defWeight;
    private final double ctrlWeight;

    /** Sentinel value indicating no PCA weight has been provided for this team. */
    public static final double NO_WEIGHT = -1.0;

    /**
     * Constructs a new {@code FileStorage} with no custom weights (style-bias fallback).
     *
     * @param s  the squad data parsed from CSV
     * @param a  the team adaptability level
     * @param st the team stamina level
     * @param f  the team's base formation
     */
    public FileStorage(Squad s, TeamAdaptability a, StaminaLevel st, Formation f) {
        this(s, a, st, f, NO_WEIGHT, NO_WEIGHT, NO_WEIGHT);
    }

    /**
     * Constructs a new {@code FileStorage} with explicit PCA-derived intent weights.
     *
     * @param s          the squad data parsed from CSV
     * @param a          the team adaptability level
     * @param st         the team stamina level
     * @param f          the team's base formation
     * @param atkWeight  PCA attack weight  (0.0–1.0), or {@link #NO_WEIGHT} to fall back
     * @param defWeight  PCA defence weight (0.0–1.0), or {@link #NO_WEIGHT} to fall back
     * @param ctrlWeight PCA control weight (0.0–1.0), or {@link #NO_WEIGHT} to fall back
     */
    public FileStorage(Squad s, TeamAdaptability a, StaminaLevel st, Formation f,
                       double atkWeight, double defWeight, double ctrlWeight) {
        this.squadData = s;
        this.adaptabilityData = a;
        this.staminaData = st;
        this.formation = f;
        this.atkWeight  = atkWeight;
        this.defWeight  = defWeight;
        this.ctrlWeight = ctrlWeight;
    }

    /**
     * Returns the squad data for this entry.
     *
     * @return the {@link Squad} containing name, manager, and style information
     */
    public Squad getSquadData() {
        return squadData;
    }

    /**
     * Returns the team adaptability level for this entry.
     *
     * @return the {@link TeamAdaptability} level
     */
    public TeamAdaptability getAdaptabilityData() {
        return adaptabilityData;
    }

    /**
     * Returns the team stamina level for this entry.
     *
     * @return the {@link StaminaLevel} of the team
     */
    public StaminaLevel getStaminaData() {
        return staminaData;
    }

    /**
     * Returns the team's base formation.
     *
     * @return the {@link Formation} parsed from CSV
     */
    public Formation getFormation() {
        return formation;
    }

    /** Returns the PCA attack weight, or {@link #NO_WEIGHT} if not set. */
    public double getAtkWeight()  { return atkWeight; }

    /** Returns the PCA defence weight, or {@link #NO_WEIGHT} if not set. */
    public double getDefWeight()  { return defWeight; }

    /** Returns the PCA control weight, or {@link #NO_WEIGHT} if not set. */
    public double getCtrlWeight() { return ctrlWeight; }

    /**
     * Returns {@code true} if all three PCA weights are present (i.e. not {@link #NO_WEIGHT}).
     * Use this to decide whether to call the custom-weight {@code Team} constructor.
     */
    public boolean hasCustomWeights() {
        return atkWeight != NO_WEIGHT && defWeight != NO_WEIGHT && ctrlWeight != NO_WEIGHT;
    }
}

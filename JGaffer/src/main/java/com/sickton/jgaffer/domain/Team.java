package com.sickton.jgaffer.domain;

import java.util.*;

/**
 * Represents a football team with its tactical attributes.
 *
 * <p>Combines a {@link Squad} identity with runtime attributes that influence tactical
 * recommendations: {@link TeamIntent} (attack/control/defence weights), {@link StaminaLevel},
 * and {@link TeamAdaptability}. The intent is automatically initialized based on the
 * squad's playing style.</p>
 *
 * @author sickton
 * @see Squad
 * @see TeamIntent
 * @see StaminaLevel
 * @see TeamAdaptability
 */
public class Team {
    private final String name;
    private final Squad squad;
    private final Formation formation;
    protected final TeamIntent intent;
    private final StaminaLevel staminaLevel;
    private final TeamAdaptability adaptability;

    /**
     * Constructs a new Team from a squad, stamina level, and adaptability.
     * The team name and formation are derived from the squad, and the tactical intent is
     * automatically initialized based on the squad's playing style.
     *
     * @param s            the squad providing team identity, style, and base formation
     * @param stamina      the stamina level of the team
     * @param adaptability the team's adaptability to tactical changes
     */
    public Team(Squad s, StaminaLevel stamina, TeamAdaptability adaptability) {
        this.squad = s;
        this.name = s.getTeam();
        this.formation = s.getBaseFormation();
        this.intent = new TeamIntent(s);
        this.staminaLevel = stamina;
        this.adaptability = adaptability;
    }

    /**
     * Constructs a new Team using explicit PCA-derived intent weights instead of the
     * style-bias formula. Use this constructor when per-team weights are available
     * from the {@code atk_weight / def_weight / ctrl_weight} CSV columns.
     *
     * <p>Weight order: {@code (attack, defence, control)} — matches
     * {@link TeamIntent#TeamIntent(double, double, double)}.</p>
     *
     * @param s            the squad providing team identity, style, and base formation
     * @param stamina      the stamina level of the team
     * @param adaptability the team's adaptability to tactical changes
     * @param atkWeight    PCA-derived attack weight  (0.0–1.0)
     * @param defWeight    PCA-derived defence weight (0.0–1.0)
     * @param ctrlWeight   PCA-derived control weight (0.0–1.0)
     */
    public Team(Squad s, StaminaLevel stamina, TeamAdaptability adaptability,
                double atkWeight, double defWeight, double ctrlWeight) {
        this.squad = s;
        this.name = s.getTeam();
        this.formation = s.getBaseFormation();
        this.intent = new TeamIntent(atkWeight, defWeight, ctrlWeight);
        this.staminaLevel = stamina;
        this.adaptability = adaptability;
    }

    /**
     * Returns the tactical intent of the team.
     *
     * @return the {@link TeamIntent} representing attack, control, and defence weights
     */
    public TeamIntent getIntent() {
        return intent;
    }

    /**
     * Returns the name of the team.
     *
     * @return the team name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the squad associated with this team.
     *
     * @return the {@link Squad} identity
     */
    public Squad getSquad() {
        return squad;
    }

    /**
     * Returns the stamina level of the team.
     *
     * @return the {@link StaminaLevel}
     */
    public StaminaLevel getStaminaLevel() {
        return staminaLevel;
    }

    /**
     * Returns the adaptability level of the team.
     *
     * @return the {@link TeamAdaptability}
     */
    public TeamAdaptability getAdaptabilityLevel() {
        return adaptability;
    }

    /**
     * Returns the team's base formation derived from the squad.
     *
     * @return the {@link Formation} the team typically plays
     */
    public Formation getFormation() {
        return formation;
    }
}

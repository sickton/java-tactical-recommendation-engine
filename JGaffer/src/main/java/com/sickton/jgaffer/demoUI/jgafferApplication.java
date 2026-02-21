package com.sickton.jgaffer.demoUI;

import com.sickton.jgaffer.domain.Formation;
import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.TacticRecommendation;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.engine.TacticalRecommendationEngine;
import com.sickton.jgaffer.openAIService.OpenAIClient;
import com.sickton.jgaffer.openAIService.TacticalExplanationService;
import com.sickton.jgaffer.utility.ApplicationParser;

import java.util.*;

/**
 * Main entry point for the JGaffer CLI application.
 *
 * <p>Provides an interactive command-line interface where users select a Premier League
 * team and fixture, view the match context at a randomly generated minute, suggest a
 * tactic, and receive the engine's recommendation with an AI-generated explanation.</p>
 *
 * <p>Orchestrates the full user flow: team selection, fixture display, tactic input,
 * engine invocation via {@link TacticalRecommendationEngine}, and explanation generation
 * via {@link TacticalExplanationService}.</p>
 *
 * @see TacticalRecommendationEngine
 * @see PremierLeagueFactory
 * @see TacticalExplanationService
 * @author sickton
 */
public class jgafferApplication {
    protected static Map<Integer, String> teams = ApplicationParser.buildTeamMapFromCsv("/PremierLeague/SquadInformation.csv");
    protected static Map<String, MatchContext> matchContextMap = PremierLeagueFactory.buildAllContexts();
    protected static Map<Integer, String> titles = ApplicationParser.parseTitles("/PremierLeague/PremierLeagueMatches.csv");
    protected static Map<String, String> teamCodes = ApplicationParser.getTeamCodeMap("/PremierLeague/SquadInformation.csv");

    public static void displayTeams() {
        System.out.println("Select the Club that you want to play with! ");
        System.out.println();
        System.out.println("+------------+-------------------+");
        System.out.println("|  Team ID   |     Team Name     |");
        System.out.println("+------------+-------------------+");
        System.out.println("|      1     |      Arsenal      |");
        System.out.println("|      2     |     Aston Villa   |");
        System.out.println("|      3     |     Bournemouth   |");
        System.out.println("|      4     |     Brentford     |");
        System.out.println("|      5     |      Brighton     |");
        System.out.println("|      6     |      Chelsea      |");
        System.out.println("|      7     |   Crystal Palace  |");
        System.out.println("|      8     |      Everton      |");
        System.out.println("|      9     |      Fulham       |");
        System.out.println("|     10     |     Liverpool     |");
        System.out.println("|     11     |  Manchester City  |");
        System.out.println("|     12     | Manchester United |");
        System.out.println("|     13     |  Newcastle United |");
        System.out.println("|     14     | Nottingham Forest |");
        System.out.println("|     15     | Tottenham Hotspur |");
        System.out.println("|     16     |  West Ham United  |");
        System.out.println("|     17     |       Wolves      |");
        System.out.println("|     18     |  Leicester City   |");
        System.out.println("|     19     |    Ipswich Town   |");
        System.out.println("|     20     |     Southampton   |");
        System.out.println("+------------+-------------------+");
        System.out.println();
        System.out.println("Enter the Team ID - ");
    }

    public static void showFixtures(String teamName)
    {
        Map<Integer, String> matches = new TreeMap<>(PremierLeagueFactory.getFixtureList(teamCodes.get(teamName)));
        System.out.println("Showing a total of : " + matches.size() + " matches");
        List<String> homeMatches = new ArrayList<>();
        List<String> awayMatches = new ArrayList<>();
        for(Map.Entry<Integer, String> entry : matches.entrySet())
        {
            if(entry.getValue().indexOf(teamCodes.get(teamName)) == 0) {
                String matchNum = "";
                if(entry.getKey() < 10)
                    matchNum = "  " + entry.getKey();
                else if(entry.getKey() < 100)
                    matchNum = " " + entry.getKey();
                else
                    matchNum = "" + entry.getKey();
                homeMatches.add(matchNum.concat(" : ".concat(entry.getValue())));
            }
            else {
                String matchNum = "";
                if(entry.getKey() < 10)
                    matchNum = "  " + entry.getKey();
                else if(entry.getKey() < 100)
                    matchNum = " " + entry.getKey();
                else
                    matchNum = "" + entry.getKey();
                awayMatches.add(matchNum.concat(" : ".concat(entry.getValue())));
            }
        }

        if(homeMatches.size() != awayMatches.size())
            throw new IllegalArgumentException("Invalid fixtures list");
        System.out.println("+-------------------------------------------+");
        System.out.println("|                 Fixtures                  |");
        System.out.println("+-------------------------------------------+");
        System.out.println("|        Home         |        Away         |");
        System.out.println("+-------------------------------------------+");
        for(int i = 0; i < homeMatches.size(); i++)
        {
            System.out.println("|    " + homeMatches.get(i) + "    |    " + awayMatches.get(i) + "    |");
        }
        System.out.println("+-------------------------------------------+");
    }

    public static void displayTactics()
    {
        System.out.println("+---------------------+");
        System.out.println("|        Tactic       |");
        System.out.println("+---------------------+");
        System.out.println("|        CONTROL      |");
        System.out.println("|     COUNTER_ATTACK  |");
        System.out.println("|      DIRECT_PLAY    |");
        System.out.println("|     GEGENPRESSING   |");
        System.out.println("|       HIGH_PRESS    |");
        System.out.println("|       LOW_BLOCK     |");
        System.out.println("|       TIKI_TAKA     |");
        System.out.println("+---------------------+");
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        displayTeams();
        int teamId = sc.nextInt();
        System.out.println("Proceeding with the selected team - " + teams.get(teamId));
        showFixtures(teams.get(teamId));
        System.out.print("Enter the Match Number to retrieve a context - ");
        Integer matchNumber = sc.nextInt();
        Team team = PremierLeagueFactory.buildTeamFromName(teams.get(teamId));
        TacticalRecommendationEngine engine = new TacticalRecommendationEngine();
        Random random = new Random();
        int minute = random.nextInt(90 - 1 + 1) + 1;
        MatchContext matchContext = matchContextMap.get(titles.get(matchNumber).concat("_".concat("" + minute)));
        System.out.println("-------------------------------------------------------------------");
        System.out.println(matchContext.toString());
        System.out.println("-------------------------------------------------------------------");
        displayTactics();
        System.out.println("Enter your tactical suggestion for " + teams.get(teamId));
        String tactic = sc.next();
        Tactic userTact = Tactic.valueOf(tactic.toUpperCase());
        TacticRecommendation recommendation = engine.recommendWithDetails(matchContext, team);
        Tactic systemTact = recommendation.getTactic();
        int confidence = recommendation.getConfidence();
        Formation suggestedFormation = recommendation.getSuggestedFormation();
        System.out.println("-------------------------------------------------------------------");
        System.out.println("Engine recommendation: " + systemTact + " — " + suggestedFormation.getLabel() + " (Confidence: " + confidence + "%)");
        System.out.println("-------------------------------------------------------------------");
        Team opponent = matchContext.getHome().getName().equalsIgnoreCase(team.getName())
                ? matchContext.getAway() : matchContext.getHome();
        String teamStyle    = team.getSquad().getTeamStyle().name();
        String oppStyle     = opponent.getSquad().getTeamStyle().name();
        String teamStamina  = team.getStaminaLevel().name();
        String teamAdapt    = team.getAdaptabilityLevel().name();
        int goalDiff;

        if (matchContext.getHome().getName().equalsIgnoreCase(team.getName())) {
            goalDiff = matchContext.getHomeGoals() - matchContext.getAwayGoals();
        } else {
            goalDiff = matchContext.getAwayGoals() - matchContext.getHomeGoals();
        }
        String scoreline    = matchContext.getHomeGoals() + "-" + matchContext.getAwayGoals();
        String gamePhase;
        if      (minute <= 15)  gamePhase = "Early Minutes (0-15)";
        else if (minute <= 44)  gamePhase = "Closing Half (16-44)";
        else if (minute <= 50)  gamePhase = "Half Time (45-50)";
        else if (minute <= 60)  gamePhase = "Build Phase (51-60)";
        else if (minute <= 70)  gamePhase = "Tension Time (61-70)";
        else if (minute <= 87)  gamePhase = "Late Game (71-87)";
        else                    gamePhase = "Stoppage Time (88+)";
        String prompt = """
You are the head coach of %s delivering a clear tactical briefing to your players.

Context:
Recommended tactic: %s
Suggested formation: %s
Opponent: %s
Team style: %s | Opponent style: %s
Stamina: %s | Adaptability: %s
Game phase: %s
Minute: %d
Score: %s
Goal difference from your perspective: %d
Engine confidence: %d%%

Assume %s is the team being managed. Interpret the score and goal difference from this team's perspective (winning, losing, or drawing).

Use plain ASCII only. No markdown, no bold text, no emojis, no smart quotes.

Give exactly 5 bullet points using a dash (-):
- Points 1-2: explain why this tactic fits the current scoreline and game phase.
- Point 3: explain the tactical matchup (style vs style and formation structure).
- Point 4: explain the main risk this tactic accepts or prevents.
- Point 5: a short, direct, imperative instruction to the team (motivational but realistic).
"""
                .formatted(
                        team.getName(),
                        systemTact,
                        suggestedFormation.getLabel(),
                        opponent.getName(),
                        teamStyle,
                        oppStyle,
                        teamStamina,
                        teamAdapt,
                        gamePhase,
                        minute,
                        scoreline,
                        goalDiff,
                        confidence,
                        team.getName()
                );

        OpenAIClient openAIClient = new OpenAIClient(System.getenv("OPENAI_API_KEY"));
        TacticalExplanationService aiexp = new TacticalExplanationService(openAIClient);
        if(userTact.equals(systemTact))
        {
            System.out.println("Yes Gaffer! I agree with you. This tactic must work, because: ");
            String exp = aiexp.generateExplanation(prompt);
            exp = exp.replace("\\u2019", "'")
                    .replace("\\u2018", "'")
                    .replace("\\u201C", "\"")
                    .replace("\\u201D", "\"");
            System.out.println(exp);
        }
        else
        {
            System.out.println("Gaffer, I think we could try out the - " + systemTact + " (" + suggestedFormation.getLabel() + ") tactic ! I think that might help us since: ");
            String exp = aiexp.generateExplanation(prompt);
            exp = exp.replace("\\u2019", "'")
                    .replace("\\u2018", "'")
                    .replace("\\u201C", "\"")
                    .replace("\\u201D", "\"");
            System.out.println(exp);
        }
    }
}

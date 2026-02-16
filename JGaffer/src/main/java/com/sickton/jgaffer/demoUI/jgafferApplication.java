package com.sickton.jgaffer.demoUI;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
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
    protected static Map<Integer, String> teams = ApplicationParser.buildTeamMap();
    protected static Map<String, MatchContext> matchContextMap = PremierLeagueFactory.buildAllContexts();
    protected static Map<Integer, String> titles = ApplicationParser.parseTitles();
    protected static Map<String, String> teamCodes = ApplicationParser.getTeamCodeMap();

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
        Tactic systemTact = engine.recommendTactic(matchContext, team);
        String prompt = """
You are a football manager explaining a tactical decision to your players.
Explain why the %s tactic is right for %s in this match.
Use simple language, plain ASCII characters only, and no markdown or formatting symbols.
Do not use bold text, emojis, smart quotes, or headings.

Match context:
%s

Give exactly 5 bullet points using a dash (-).
The first 4 explain the reasoning.
The 5th is a short passionate spoken instruction from the manager to the team.
"""
                .formatted(systemTact, teams.get(teamId), matchContext.toString());

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
            System.out.println("Gaffer, I think we could try out the - " + systemTact + " tactic ! I think that might help us since: ");
            String exp = aiexp.generateExplanation(prompt);
            exp = exp.replace("\\u2019", "'")
                    .replace("\\u2018", "'")
                    .replace("\\u201C", "\"")
                    .replace("\\u201D", "\"");
            System.out.println(exp);
        }
    }
}

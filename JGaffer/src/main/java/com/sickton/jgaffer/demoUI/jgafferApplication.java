package com.sickton.jgaffer.demoUI;

import com.sickton.jgaffer.domain.MatchContext;
import com.sickton.jgaffer.domain.Tactic;
import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.engine.TacticalRecommendationEngine;
import com.sickton.jgaffer.utility.ApplicationParser;

import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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
        Map<Integer, String> matches = PremierLeagueFactory.getFixtureList(teamCodes.get(teamName));
        System.out.println("Showing a total of : " + matches.size() + " matches");
        for(Map.Entry<Integer, String> entry : matches.entrySet())
        {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
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
        Tactic userTact = Tactic.valueOf(tactic);
        Tactic systemTact = engine.recommendTactic(matchContext, team);
        String prompt = """
                        You are a football tactical analyst.
                        Explain why the %s tactic is recommended in this situation.
                        Assume that the reader has nearly no knowledge of how a football game works.
                        Make it simpler for the reader.
                        I am playing as %s team and the recommended tactic is for the team I am playing.
                        Match details:
                            %s
                        Explain in 4–5 sentences using football language.
                        """
                        .formatted(systemTact, matchContext.toString(), teams.get(teamId));
        OpenAIClient openAIClient = new OpenAIClient(System.getenv("OPENAI_API_KEY"));
        TacticalExplanationService aiexp = new TacticalExplanationService(openAIClient);
        if(userTact.equals(systemTact))
        {
            System.out.println("Congrats! The system matched the tactical recommendation with yours!");
            System.out.println(aiexp.generateExplanation(prompt));
        }
        else
        {
            System.out.println("Sorry, the system recommends - " + systemTact + "!");
            System.out.println(aiexp.generateExplanation(prompt));
        }
    }
}

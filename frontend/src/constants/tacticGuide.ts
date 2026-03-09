export interface TacticInfo {
  emoji: string;
  tagline: string;
  whatIsIt: string;
  realLife: string;
  whenItWorks: string;
  whenItDoesnt: string;
  analogyEmoji: string;
  analogy: string;
}

export const TACTIC_GUIDE: Record<string, TacticInfo> = {
  GEGENPRESSING: {
    emoji: '⚡',
    tagline: 'The moment you lose the ball, everyone chases it back immediately.',
    whatIsIt:
      'As soon as you give the ball away, every single player sprints toward it — no retreating, no reorganising. You swarm the opponent before they have a second to think, forcing a rushed pass or a mistake right away.',
    realLife:
      "Made famous by Liverpool under Jurgen Klopp. The whole team hunted the ball back together like a unit, often winning it back within seconds in dangerous positions close to the opponent's goal.",
    whenItWorks:
      'When your players have plenty of energy. Best for disrupting a team that has just won the ball and is still disorganised.',
    whenItDoesnt:
      'Very draining. If your team is tired or it is late in the match, you will leave huge gaps behind you that fast opponents will exploit.',
    analogyEmoji: '🐝',
    analogy:
      'Like a swarm of bees — the moment the hive is threatened, every bee attacks at once and overwhelms the threat before it can settle.',
  },

  HIGH_PRESS: {
    emoji: '🔥',
    tagline: 'Push your whole team up the pitch and get in their faces early.',
    whatIsIt:
      "Your players push high toward the opponent's end and aggressively close down their defenders and goalkeeper. You are trying to force them into a panicked kick or a mistake, rather than letting them play out calmly.",
    realLife:
      "Classic Pep Guardiola. His Manchester City strikers do not wait up front — they press the opposition goalkeeper so aggressively that opponents often kick the ball straight to a City player.",
    whenItWorks:
      'Best early in the game when energy is high, or against teams that prefer to pass from the back. If you can pin them in their own half, they can never get comfortable.',
    whenItDoesnt:
      'If the opponent has fast forwards and breaks your press with one pass, there is a lot of empty space behind your defensive line for them to run into.',
    analogyEmoji: '🏇',
    analogy:
      "Like blocking someone's driveway — they can't build up speed and are forced to make rushed decisions before they're ready.",
  },

  TIKI_TAKA: {
    emoji: '🎯',
    tagline: 'Keep the ball moving. Make them run after you until they tire.',
    whatIsIt:
      'Short, quick passes between your players at a high tempo. You are not rushing to score — you are making the opponent exhaust themselves chasing the ball. Once they are tired and gaps appear, you strike.',
    realLife:
      "Barcelona between 2008 and 2012 perfected this. Spain's national team used it to win the 2010 World Cup. The logic is simple: if you have the ball, they can't score.",
    whenItWorks:
      'Brilliant when you are ahead and want to protect your lead. Also very effective when the opponent is chasing the game and leaving space — you pass around them while they tire themselves out.',
    whenItDoesnt:
      'Too slow when you are losing late in the game. If you need a goal urgently, patient passing will not create enough danger in time.',
    analogyEmoji: '🎪',
    analogy:
      'Keep-away in the playground. You are not trying to score every second — you are making the other side run around until they are too exhausted to defend properly.',
  },

  CONTROL: {
    emoji: '🧩',
    tagline: 'Compact, disciplined, and very hard to break down.',
    whatIsIt:
      'Everyone stays in their shape. You do not gamble, you do not press wildly, and you do not rush forward. The team stays tight and compact, and you build attacks carefully without taking unnecessary risks.',
    realLife:
      'Arsenal under Arsene Wenger in the early 2000s were the masters of this — patient build-up combined with a disciplined defensive shape that was almost impossible to disrupt.',
    whenItWorks:
      'Excellent when the score is level and there is still time to play. Also effective when the opponent is hard to read — a compact shape limits their options.',
    whenItDoesnt:
      'If you are losing late in the game, this is too cautious. You will not create enough urgency or chances when you actually need a goal.',
    analogyEmoji: '♟️',
    analogy:
      'Like playing chess — you are not going for a quick win. You set up your pieces carefully, wait for the opponent to make an error, and then capitalise.',
  },

  COUNTER_ATTACK: {
    emoji: '🚀',
    tagline: 'Let them come forward. Then hit them hard in the space they leave behind.',
    whatIsIt:
      'You sit back and absorb the opponent\'s pressure. The moment you win the ball, your fastest players sprint into the empty space behind the opponent\'s defensive line — reaching goal before they can recover.',
    realLife:
      "Leicester City's legendary 2016 title win was built almost entirely on this. They defended deep and then released Jamie Vardy's pace to devastating effect — game after game.",
    whenItWorks:
      'Perfect against stronger teams that naturally attack a lot. Also the right call when protecting a lead — let them push forward, then punish them on the break.',
    whenItDoesnt:
      'Useless if the opponent also sits back. If they do not commit players forward, there is no space behind them to run into.',
    analogyEmoji: '🥊',
    analogy:
      'Like a boxer using rope-a-dope — you absorb the punches, let them tire themselves out swinging, then land a devastating counter when they are overextended.',
  },

  DIRECT_PLAY: {
    emoji: '🏹',
    tagline: 'Skip the build-up. Get the ball to your strikers as fast as possible.',
    whatIsIt:
      'No long passing sequences — you go straight to your forwards with long balls and quick switches. It is physical, simple, and immediate. Less possession, more directness.',
    realLife:
      'Burnley, Watford, and many clubs without large budgets used this brilliantly to punch above their weight. Strong centre-forwards, aerial duels, and quick forward deliveries.',
    whenItWorks:
      'Very effective when you have a powerful striker who wins physical duels and holds the ball up. Also great late in a game when time is running out and every second matters.',
    whenItDoesnt:
      'Against a well-organised, compact defence, long balls are simple to defend and head away. It also depends heavily on having the right type of striker.',
    analogyEmoji: '🛣️',
    analogy:
      'Taking the motorway instead of the scenic road. You sacrifice elegance for speed — the only thing that matters is getting there fast.',
  },

  LOW_BLOCK: {
    emoji: '🧱',
    tagline: 'Drop deep, cover every gap, and make it impossible to score.',
    whatIsIt:
      'Your whole team retreats into your own half — every gap covered, every channel blocked. You are not trying to dominate the game. You are making it physically impossible for the opponent to find a way through.',
    realLife:
      "The famous 'park the bus'. Atletico Madrid do a sophisticated version of this even against elite clubs. In 2021, they knocked out Manchester City by defending brilliantly and punishing them on the counter.",
    whenItWorks:
      'Essential when you are winning against a stronger team and just need to survive the final minutes. Also the right call when your players are exhausted — it is easier to defend in a low block than to press.',
    whenItDoesnt:
      'Very hard to score from here. If you are already losing, this will not help you get back into the game — you need to take more risks, not fewer.',
    analogyEmoji: '🏰',
    analogy:
      'Defending a castle. You do not chase the enemy across the battlefield — you pull up the drawbridge, man the walls, and make it impossible for anyone to get through.',
  },
};

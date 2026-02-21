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
    tagline: 'Win the ball back immediately — no mercy.',
    whatIsIt: 'Gegenpressing means pressing the opponent the instant you lose the ball. Instead of retreating to defend, every player sprints to hunt the ball back right away, usually within the first 5 seconds after losing possession.',
    realLife: "This is exactly what Jurgen Klopp's Liverpool became famous for. When Liverpool lost the ball, the whole team swarmed the opponent instantly, often winning it back in dangerous positions. The idea came from Klopp's time at Borussia Dortmund. He called it 'the best playmaker in the world' — because winning the ball high up the pitch means you're already close to goal.",
    whenItWorks: "Best when your team has high stamina and you're either drawing or losing. You need to attack the opponent's disorganisation right after they get the ball — when they haven't settled yet.",
    whenItDoesnt: "Extremely draining. If your players are tired or it's late in the game, gaps open up and you'll get caught out badly.",
    analogyEmoji: '🐝',
    analogy: 'Think of it like a swarm of bees — the moment the hive is threatened, every bee attacks immediately and overwhelms the threat.',
  },
  HIGH_PRESS: {
    emoji: '🔥',
    tagline: 'Push up the pitch and force mistakes early.',
    whatIsIt: "High press means your whole team pushes high up the pitch and pressures the opponent's defenders and goalkeeper directly. You're trying to force them into a mistake or a long, hopeless kick under pressure.",
    realLife: "Pep Guardiola's Manchester City do this constantly. Their strikers don't just stand up front waiting — they press the opposition goalkeeper and centre-backs so aggressively that the opponents often kick the ball directly to a City player. It's also how Brighton under Roberto De Zerbi disrupted bigger clubs.",
    whenItWorks: "Ideal early in a game when energy is high, or when you're playing a team that likes to pass from the back. If you can pin them in their own half, they can never get comfortable.",
    whenItDoesnt: "Risky against teams with fast strikers — if your press is broken by a quick pass, there's a lot of empty space behind your defenders to exploit.",
    analogyEmoji: '🏇',
    analogy: "Like blocking someone's driveway — they can't build speed and have to make rushed decisions before they're ready.",
  },
  TIKI_TAKA: {
    emoji: '🎯',
    tagline: 'Keep the ball, control the game, make them chase.',
    whatIsIt: "Tiki-taka is short, rapid passing that keeps possession for long spells. You're not rushing to attack — you're making the opponent run around chasing the ball until gaps appear naturally, then you strike.",
    realLife: "The legendary Barcelona team of 2008–2012 (with Xavi, Iniesta, and Messi) perfected this. Spain's national team used it to win the 2010 World Cup. The idea is that if you have the ball, the other team can't score. Fans sometimes found it boring to watch — but it was devastatingly effective.",
    whenItWorks: "When you're winning and want to protect the lead. Also brilliant when the opponent is chasing the game and leaving space — you just pass around them while they tire themselves out.",
    whenItDoesnt: "If your team is losing by more than one goal with little time left, keeping the ball patiently is too slow. You need to take more risks.",
    analogyEmoji: '🎪',
    analogy: "Imagine keep-away in the playground. You're not trying to score every second — you're making the other kid run around until they're exhausted and make a mistake.",
  },
  CONTROL: {
    emoji: '🧩',
    tagline: 'Disciplined, structured, and difficult to beat.',
    whatIsIt: "Control is about being solid and organised in both attack and defence. You don't gamble, you don't press recklessly, and you don't rush. Every player knows their role, the team shape stays compact, and you build attacks patiently.",
    realLife: "Arsenal under Arsene Wenger in the early 2000s were the masters of this — patient build-up play combined with a disciplined defensive shape. More recently, Brighton have shown how structured possession play can outthink bigger clubs.",
    whenItWorks: "Excellent when the score is level and there's still time to play. Also effective when the opponent is unpredictable — a compact shape limits their options.",
    whenItDoesnt: "If you're losing late in the game, control won't create enough urgency. You might run down the clock too safely and never create the chances you need.",
    analogyEmoji: '♟️',
    analogy: "Like playing chess — you're not trying to win in 5 moves. You set up your pieces carefully, wait for the opponent to make an error, and then capitalise.",
  },
  COUNTER_ATTACK: {
    emoji: '🚀',
    tagline: 'Let them attack — then hit them on the break.',
    whatIsIt: "Counter-attacking means sitting back, absorbing pressure, and waiting for the opponent to come forward. The moment you win the ball, you transition instantly — fast runners sprinting into the space the opponent has left behind their defensive line.",
    realLife: "Jose Mourinho's Chelsea and Inter Milan were legends of this. Also, Leicester City's incredible 2015/16 title win was built almost entirely on counter-attacks — Jamie Vardy's blistering pace terrorised every team. The goal was always the same: defend deep, then explode forward.",
    whenItWorks: "Perfect when you're playing a stronger team that will naturally attack you. Or late in a game when you're holding a lead — let them come, then punish them on the break.",
    whenItDoesnt: "If the opponent is also sitting deep, there's no space to counter into. You need the other team to commit players forward first.",
    analogyEmoji: '🥊',
    analogy: "Like a boxer using the rope-a-dope — you absorb the opponent's attacks, let them tire themselves out, then land a devastating counter when they're overextended.",
  },
  DIRECT_PLAY: {
    emoji: '🎯',
    tagline: 'Simple, fast, and straight to the point.',
    whatIsIt: "Direct play means skipping the fancy passing and going straight for goal — long balls over the top, quick switches of play, and getting the ball to your strikers as quickly as possible. Less possession, more directness.",
    realLife: "Watford, Burnley, and many lower-budget Premier League clubs have used this brilliantly. When you don't have the technical quality to out-pass the opponent, you use power and physicality instead. Sam Allardyce's teams were textbook examples — strong centre-forwards, aerial battles, and direct deliveries.",
    whenItWorks: "Very effective when you have a powerful striker who wins headers and holds the ball up. Also great late in a game when time is running out and you need a goal — you go direct because every second counts.",
    whenItDoesnt: "Against deep, well-organised defences, long balls are easy to defend. It also relies heavily on the quality of your target striker.",
    analogyEmoji: '🏹',
    analogy: "Like taking the motorway instead of scenic country roads. You sacrifice elegance for speed — you just want to get there fast.",
  },
  LOW_BLOCK: {
    emoji: '🧱',
    tagline: 'Park the bus. Defend everything. Frustrate them.',
    whatIsIt: "Low block means everyone drops deep behind the ball and you pack your own half with players. You're not trying to dominate — you're trying to make it absolutely impossible for the opponent to find a way through. Every gap is covered.",
    realLife: "The famous 'park the bus' approach. Mourinho's teams did it. Neil Warnock's Cardiff. Burnley under Sean Dyche. Atletico Madrid under Diego Simeone do a sophisticated version of this even against the best clubs in Europe. In 2021, Atletico knocked out Manchester City by defending brilliantly and hitting them on the counter.",
    whenItWorks: "Essential when you're winning against a stronger team and just need to survive the final minutes. Also the right call when your team is exhausted — it's physically easier to defend in a low block than to chase the ball.",
    whenItDoesnt: "It's very hard to score from a low block. If you're losing, this won't help you get back into the game — you need to take more risks.",
    analogyEmoji: '🏰',
    analogy: "Like defending a castle. You don't chase the enemy across the battlefield — you pull up the drawbridge, man the walls, and make it impossible for them to get in.",
  },
};

/**
 * Computes a normalised (dx, dy) positional offset for each escape-team
 * player based on where the ball currently is.
 *
 * Coordinate frame:
 *   x: 0 = own goal end → 1 = opposition end
 *   y: 0 = top touchline → 1 = bottom touchline
 */
export function computeEscapeShift(
  id: string,
  ballNormX: number,
  ballNormY: number,
  baseX: number,
  baseY: number,
): [number, number] {
  const isLeftBall  = ballNormY < 0.40;
  const isRightBall = ballNormY > 0.60;
  const isWide      = isLeftBall || isRightBall;
  const ballDy      = ballNormY - baseY;
  const ballDx      = ballNormX - baseX;
  const nearSide    = isLeftBall ? baseY < 0.50 : baseY > 0.50;

  switch (id) {
    // GK: shuffle toward ball side for easy distribution
    case 'GK':
      return [0, ballDy * 0.12];

    // Centre backs: near-side steps up, far-side tucks in; centre CB stays compact
    case 'CB':
    case 'CB2':
    case 'CB3': {
      if (isWide) {
        return nearSide
          ? [0.04, ballDy * 0.20]
          : [0.02, (0.50 - baseY) * 0.10];
      }
      return [0.02, (0.50 - baseY) * 0.08];
    }

    // Fullbacks / wingbacks
    case 'LB':
    case 'RB':
    case 'LWB':
    case 'RWB': {
      if (nearSide) return [-0.05, ballDy * 0.22];
      return [0.03, (0.50 - baseY) * 0.12];
    }

    // CDM: drops deeper, drifts toward ball
    case 'CDM':
    case 'CDM2':
      return [-0.09 - Math.abs(ballDx) * 0.06, ballDy * 0.26];

    // CMs: near-side tucks inside + slightly forward; far-side holds
    case 'CM':
    case 'CM2':
    case 'LCM':
    case 'RCM': {
      if (nearSide) return [0.04, (0.50 - baseY) * 0.28 + ballDy * 0.12];
      return [0.01, (0.50 - baseY) * 0.08];
    }

    // CAM: pushes up, drifts to weak side to stretch defence
    case 'CAM':
    case 'AM':
      return [
        0.07 + ballDx * 0.08,
        isWide ? (0.50 - baseY) * 0.30 : ballDy * 0.12,
      ];

    // Wingers: near-side drops for receiving angle; far-side holds/pushes
    case 'LW':
    case 'LM': {
      const isNear = isLeftBall;
      return isNear
        ? [-0.06, ballDy * 0.18]
        : [0.06, (0.50 - baseY) * 0.18];
    }
    case 'RW':
    case 'RM': {
      const isNear = isRightBall;
      return isNear
        ? [-0.06, ballDy * 0.18]
        : [0.06, (0.50 - baseY) * 0.18];
    }

    // Striker: pushes in behind CB line, stays central
    case 'ST':
    case 'CF':
    case 'SS':
      return [0.08 + ballDx * 0.05, (0.50 - baseY) * 0.14];

    default:
      return [ballDy * 0.06, ballDy * 0.08];
  }
}

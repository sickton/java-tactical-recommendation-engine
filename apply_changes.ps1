$base = 'C:\Users\sriva\OneDrive\Desktop\java-tactic-recommendation-system\java-tactical-recommendation-engine\JGaffer\src\main\java\com\sickton\jgaffer\rules\game_phases'

# FILE 1: EarlyMinuteTactics.java
$file = $base + '\EarlyMinuteTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_EARLY);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_EARLY);')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 1 EarlyMinuteTactics: found $before occurrences, confirmed $after replacements"

# FILE 2: ClosingHalfTactics.java
$file = $base + '\ClosingHalfTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_CLOSING);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_CLOSING);')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 2 ClosingHalfTactics: found $before occurrences, confirmed $after replacements"

# FILE 3: HalfTimeTactics.java
$file = $base + '\HalfTimeTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_HALFTIME);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_HALFTIME);')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 3 HalfTimeTactics: found $before occurrences, confirmed $after replacements"

# FILE 4A: BuildPhaseTactics.java - opponent scale
$file = $base + '\BuildPhaseTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_BUILD);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_BUILD);')).Count - 1
Write-Host "FILE 4A BuildPhaseTactics opp scale: found $before occurrences, confirmed $after replacements"

# FILE 4B: BuildPhaseTactics.java - adaptabilityFactor
$old = 'dControl *= adaptabilityFactor;'
$new = "dControl *= adaptabilityFactor;`r`n        dAttack  *= adaptabilityFactor;"
$before = ($content -split [regex]::Escape($old)).Count - 1
$content = $content.Replace($old, $new)
$after = ($content -split [regex]::Escape('dAttack  *= adaptabilityFactor;')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 4B BuildPhaseTactics adaptFactor: found $before occurrences of old, confirmed $after new dAttack lines"

# FILE 5A: TensionTimeTactics.java - opponent scale
$file = $base + '\TensionTimeTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_TENSION);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_TENSION);')).Count - 1
Write-Host "FILE 5A TensionTimeTactics opp scale: found $before occurrences, confirmed $after replacements"

# FILE 5B: TensionTimeTactics.java - adaptFactor
$old = 'dControl *= adaptFactor;'
$new = "dControl *= adaptFactor;`r`n        dAttack  *= adaptFactor;"
$before = ($content -split [regex]::Escape($old)).Count - 1
$content = $content.Replace($old, $new)
$after = ($content -split [regex]::Escape('dAttack  *= adaptFactor;')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 5B TensionTimeTactics adaptFactor: found $before occurrences of old, confirmed $after new dAttack lines"

# FILE 6A: LateGameTactics.java - opponent scale
$file = $base + '\LateGameTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_LATE);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_LATE);')).Count - 1
Write-Host "FILE 6A LateGameTactics opp scale: found $before occurrences, confirmed $after replacements"

# FILE 6B: LateGameTactics.java - adaptFactor
$old = 'dControl *= adaptFactor;'
$new = "dControl *= adaptFactor;`r`n        dAttack  *= adaptFactor;"
$before = ($content -split [regex]::Escape($old)).Count - 1
$content = $content.Replace($old, $new)
$after = ($content -split [regex]::Escape('dAttack  *= adaptFactor;')).Count - 1
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 6B LateGameTactics adaptFactor: found $before occurrences of old, confirmed $after new dAttack lines"

# FILE 7A: StoppageTimeTactics.java - opponent scale
$file = $base + '\StoppageTimeTactics.java'
$content = Get-Content $file -Raw
$before = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas);')).Count - 1
$content = $content.Replace('applyOpponentStyleAdjustments(opponentStyle, deltas);', 'applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_STOPPAGE);')
$after = ($content -split [regex]::Escape('applyOpponentStyleAdjustments(opponentStyle, deltas, OPP_SCALE_STOPPAGE);')).Count - 1
Write-Host "FILE 7A StoppageTimeTactics opp scale: found $before occurrences, confirmed $after replacements"
[System.IO.File]::WriteAllText($file, $content)
Write-Host "FILE 7 StoppageTimeTactics: all changes written."

Write-Host "`nAll replacements complete."

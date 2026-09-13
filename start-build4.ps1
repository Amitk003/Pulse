$p = "C:/Program Files/Unity/Hub/Editor/6000.6.0f1/Editor/Unity.exe"
$root = "C:\Users\amitk\My project (1)"
$log = "C:\Users\amitk\My project (1)\Pulse\unity-build4.log"
$args = @(
  "-batchmode",
  "-nographics",
  "-projectPath",
  "$root",
  "-executeMethod",
  "AdventureGame.Editor.PulseAndroidExport.BuildArenaApk",
  "-logFile",
  "$log",
  "-quit"
)
Start-Process -FilePath $p -ArgumentList $args -WorkingDirectory $root

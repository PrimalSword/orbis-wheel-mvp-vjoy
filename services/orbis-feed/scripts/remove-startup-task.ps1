$ErrorActionPreference = "Stop"
$TaskName = "OrbisFeed"
Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
Write-Host "Tarefa $TaskName removida."

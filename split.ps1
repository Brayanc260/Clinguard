$content = Get-Content -Raw -Path "src\main\resources\static\index.html" -Encoding UTF8

$cssRegex = '(?s)<style>(.*?)</style>'
$jsRegex = '(?s)<script>(.*?)</script>'

$cssMatch = [regex]::Match($content, $cssRegex)
$cssContent = if ($cssMatch.Success) { $cssMatch.Groups[1].Value.Trim() } else { "" }

$jsMatch = [regex]::Match($content, $jsRegex)
$jsContent = if ($jsMatch.Success) { $jsMatch.Groups[1].Value.Trim() } else { "" }

$newContent = $content -replace '(?s)<style>.*?</style>', '<link rel="stylesheet" href="css/styles.css">'
$newContent = $newContent -replace '(?s)<script>.*?</script>', '<script src="js/app.js"></script>'

New-Item -ItemType Directory -Force -Path "src\main\resources\static\css" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\resources\static\js" | Out-Null

[IO.File]::WriteAllText("src\main\resources\static\css\styles.css", $cssContent, [Text.Encoding]::UTF8)
[IO.File]::WriteAllText("src\main\resources\static\js\app.js", $jsContent, [Text.Encoding]::UTF8)
[IO.File]::WriteAllText("src\main\resources\static\index.html", $newContent, [Text.Encoding]::UTF8)

Write-Host "Files successfully split."

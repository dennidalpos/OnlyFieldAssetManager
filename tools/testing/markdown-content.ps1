param([string]$Reports = "$PSScriptRoot/../../shared/exchange/build/reports")
$ErrorActionPreference = 'Stop'
$expected = 'user|\`ticks`` ##INJECTED <em>html</em>&amp;[link](x)*_~'
foreach ($language in 'it', 'en', 'es') {
    $control = [xml]('<root>' + (ConvertFrom-Markdown -Path "$Reports/markdown-control-$language.md").Html + '</root>')
    $escaped = [xml]('<root>' + (ConvertFrom-Markdown -Path "$Reports/markdown-escaped-$language.md").Html + '</root>')
    foreach ($tag in 'h1', 'h2', 'h3', 'li', 'strong', 'em', 'a', 'pre', 'table') {
        if ($control.SelectNodes("//$tag").Count -ne $escaped.SelectNodes("//$tag").Count) { throw "$language changed $tag structure" }
    }
    $controlTables = $control.SelectNodes('//table')
    $escapedTables = $escaped.SelectNodes('//table')
    for ($table = 0; $table -lt $controlTables.Count; $table++) {
        $controlRows = $controlTables[$table].SelectNodes('.//tr')
        $escapedRows = $escapedTables[$table].SelectNodes('.//tr')
        if ($controlRows.Count -ne $escapedRows.Count) { throw "$language changed table $table rows" }
        for ($row = 0; $row -lt $controlRows.Count; $row++) {
            if ($controlRows[$row].ChildNodes.Count -ne $escapedRows[$row].ChildNodes.Count) { throw "$language changed table $table row $row columns" }
            foreach ($cell in $escapedRows[$row].ChildNodes) {
                if ($cell.InnerText.Contains('user') -and !$cell.InnerText.Contains($expected)) { throw "$language lost cell content: $($cell.InnerText)" }
            }
        }
    }
    foreach ($node in $escaped.SelectNodes('//h1 | //h3 | //li')) {
        if ($node.InnerText.Contains('user') -and !$node.InnerText.Contains($expected)) { throw "$language lost heading/list content" }
    }
    Write-Output "PASS $language`: $($escapedTables.Count) tables, structure and literal user content preserved"
}
$codeFiles = @(Get-ChildItem -LiteralPath $Reports -Filter 'markdown-code-*.md')
if ($codeFiles.Count -ne 7) { throw 'Code span fixtures missing: run MarkdownEscapingTest first' }
foreach ($file in $codeFiles) {
    $expected = Get-Content -LiteralPath ([IO.Path]::ChangeExtension($file.FullName, '.txt')) -Raw -Encoding utf8
    $document = [System.Xml.XmlDocument]::new()
    $document.PreserveWhitespace = $true
    $document.LoadXml('<root>' + (ConvertFrom-Markdown -Path $file.FullName).Html + '</root>')
    $value = $document.SelectNodes('//table')[1].SelectNodes('.//tbody/tr/td')[1].InnerText
    if ($value -cne $expected) { throw "Code span content changed: $($file.Name): <$value> / <$expected>" }
    if (!($document.SelectNodes('//li') | Where-Object { $_.InnerText.Contains("($expected)") })) { throw "Filename content changed: $($file.Name)" }
}
Write-Output 'PASS code span boundaries: pipes, backslashes, backtick runs, spaces and CR/LF'

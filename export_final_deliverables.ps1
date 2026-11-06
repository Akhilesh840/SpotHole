$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0 # wdAlertsNone

try {
    $docPath = (Resolve-Path "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).docx").Path
    $pdfPath = [System.IO.Path]::ChangeExtension($docPath, ".pdf")
    $tempPath = [System.IO.Path]::Combine((Get-Location).Path, "temp_final_clean.docx")
    
    # Open document
    $doc = $word.Documents.Open($docPath, $false, $false, $false, [Type]::Missing, [Type]::Missing, $false, [Type]::Missing, [Type]::Missing, [Type]::Missing, [Type]::Missing, $false, $true)
    Write-Host "Success! Opened in Word cleanly. Paragraphs: $($doc.Paragraphs.Count)"
    
    # Save standard transitional docx to tempPath
    $doc.SaveAs2($tempPath, 12) # 12 = wdFormatXMLDocument
    Write-Host "Saved standard transitional docx to tempPath"
    
    # Export PDF
    $doc.ExportAsFixedFormat($pdfPath, 17) # 17 = wdExportFormatPDF
    Write-Host "Exported PDF to: $pdfPath"
    
    $doc.Close([ref]$false)
    $word.Quit()
    $word = $null
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
    
    Start-Sleep -Milliseconds 500
    Move-Item -Path $tempPath -Destination $docPath -Force
    Write-Host "Replaced final docx with standardized transitional docx: $docPath"
} catch {
    Write-Host "Error: $_"
} finally {
    if ($word -ne $null) {
        $word.Quit()
    }
}

param(
    [string]$OutputDir = (Join-Path (Get-Location) '毕设文档\picture\第一批软件工程图')
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$Theme = @{
    BlueFill = '#dae8fc'
    BlueStroke = '#6c8ebf'
    GreenFill = '#d5e8d4'
    GreenStroke = '#82b366'
    YellowFill = '#fff2cc'
    YellowStroke = '#d6b656'
    OrangeFill = '#ffe6cc'
    OrangeStroke = '#d79b00'
    PurpleFill = '#e1d5e7'
    PurpleStroke = '#9673a6'
    RedFill = '#f8cecc'
    RedStroke = '#b85450'
    GreyFill = '#f5f5f5'
    GreyStroke = '#666666'
    Text = '#303133'
}

function Ensure-Dir([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Xml-Escape([string]$Text) {
    if ($null -eq $Text) { return '' }
    $normalized = Normalize-Text $Text
    return $normalized.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;').Replace('"', '&quot;').Replace("`r", '').Replace("`n", '&#xa;')
}

function Normalize-Text([string]$Text) {
    if ($null -eq $Text) { return '' }
    return $Text.Replace('`r`n', "`r`n").Replace('`n', "`n").Replace('`r', "`r")
}

function Color([string]$Hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($Hex)
}

function New-Canvas([int]$Width, [int]$Height) {
    $bmp = New-Object System.Drawing.Bitmap $Width, $Height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.Color]::White)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function New-Font([float]$Size, [string]$Style = 'Regular') {
    return New-Object System.Drawing.Font('Microsoft YaHei', $Size, [System.Drawing.FontStyle]::$Style, [System.Drawing.GraphicsUnit]::Pixel)
}

function New-Pen([string]$Hex, [float]$Width = 2, [bool]$Arrow = $false, [bool]$Dashed = $false) {
    $pen = New-Object System.Drawing.Pen (Color $Hex), $Width
    if ($Arrow) {
        $pen.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(7, 9, $false)
    }
    if ($Dashed) {
        $pen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
    }
    return $pen
}

function New-Brush([string]$Hex) {
    return New-Object System.Drawing.SolidBrush (Color $Hex)
}

function New-Format([string]$Alignment = 'Center', [string]$LineAlignment = 'Center') {
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::$Alignment
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::$LineAlignment
    return $fmt
}

function Draw-Label($Graphics, [string]$Text, [int]$X, [int]$Y, [int]$W, [int]$H, [float]$Size = 18, [string]$Style = 'Regular', [string]$Hex = '#303133', [string]$Align = 'Center', [string]$LineAlign = 'Center') {
    $font = New-Font $Size $Style
    $brush = New-Brush $Hex
    $fmt = New-Format $Align $LineAlign
    $rect = New-Object System.Drawing.RectangleF ([single]$X), ([single]$Y), ([single]$W), ([single]$H)
    $Graphics.DrawString((Normalize-Text $Text), $font, $brush, $rect, $fmt)
    $fmt.Dispose()
    $font.Dispose()
    $brush.Dispose()
}

function Draw-Grid($Graphics, [int]$Width, [int]$Height) {
    $pen = New-Object System.Drawing.Pen (Color '#f1f3f5'), 1
    for ($x = 0; $x -le $Width; $x += 40) {
        $Graphics.DrawLine($pen, $x, 0, $x, $Height)
    }
    for ($y = 0; $y -le $Height; $y += 40) {
        $Graphics.DrawLine($pen, 0, $y, $Width, $y)
    }
    $pen.Dispose()
}

function New-RoundedPath([int]$X, [int]$Y, [int]$W, [int]$H, [int]$Radius = 18) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $Radius * 2
    $path.AddArc($X, $Y, $d, $d, 180, 90)
    $path.AddArc($X + $W - $d, $Y, $d, $d, 270, 90)
    $path.AddArc($X + $W - $d, $Y + $H - $d, $d, $d, 0, 90)
    $path.AddArc($X, $Y + $H - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-RoundRect($Graphics, $Node) {
    $path = New-RoundedPath $Node.X $Node.Y $Node.W $Node.H 16
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $Graphics.FillPath($fill, $path)
    $Graphics.DrawPath($pen, $path)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 8) ($Node.W - 20) ($Node.H - 16) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
    $path.Dispose()
}

function Draw-Rect($Graphics, $Node) {
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $Graphics.FillRectangle($fill, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.DrawRectangle($pen, $Node.X, $Node.Y, $Node.W, $Node.H)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 8) ($Node.W - 20) ($Node.H - 16) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Ellipse($Graphics, $Node) {
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $Graphics.FillEllipse($fill, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.DrawEllipse($pen, $Node.X, $Node.Y, $Node.W, $Node.H)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 8) ($Node.W - 20) ($Node.H - 16) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Diamond($Graphics, $Node) {
    $pts = @(
        [System.Drawing.PointF]::new([single]($Node.X + ($Node.W / 2)), [single]$Node.Y),
        [System.Drawing.PointF]::new([single]($Node.X + $Node.W), [single]($Node.Y + ($Node.H / 2))),
        [System.Drawing.PointF]::new([single]($Node.X + ($Node.W / 2)), [single]($Node.Y + $Node.H)),
        [System.Drawing.PointF]::new([single]$Node.X, [single]($Node.Y + ($Node.H / 2)))
    )
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $Graphics.FillPolygon($fill, $pts)
    $Graphics.DrawPolygon($pen, $pts)
    Draw-Label $Graphics $Node.Label ($Node.X + 15) ($Node.Y + 12) ($Node.W - 30) ($Node.H - 24) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Parallelogram($Graphics, $Node) {
    $offset = [int]([Math]::Min(28, $Node.W * 0.15))
    $pts = @(
        [System.Drawing.PointF]::new([single]($Node.X + $offset), [single]$Node.Y),
        [System.Drawing.PointF]::new([single]($Node.X + $Node.W), [single]$Node.Y),
        [System.Drawing.PointF]::new([single]($Node.X + $Node.W - $offset), [single]($Node.Y + $Node.H)),
        [System.Drawing.PointF]::new([single]$Node.X, [single]($Node.Y + $Node.H))
    )
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $Graphics.FillPolygon($fill, $pts)
    $Graphics.DrawPolygon($pen, $pts)
    Draw-Label $Graphics $Node.Label ($Node.X + 12) ($Node.Y + 8) ($Node.W - 24) ($Node.H - 16) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Cylinder($Graphics, $Node) {
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.4
    $ellipseHeight = [int]([Math]::Max(18, $Node.H * 0.22))
    $Graphics.FillRectangle($fill, $Node.X, $Node.Y + [int]($ellipseHeight / 2), $Node.W, $Node.H - $ellipseHeight)
    $Graphics.FillEllipse($fill, $Node.X, $Node.Y, $Node.W, $ellipseHeight)
    $Graphics.FillEllipse($fill, $Node.X, $Node.Y + $Node.H - $ellipseHeight, $Node.W, $ellipseHeight)
    $Graphics.DrawEllipse($pen, $Node.X, $Node.Y, $Node.W, $ellipseHeight)
    $Graphics.DrawLine($pen, $Node.X, $Node.Y + [int]($ellipseHeight / 2), $Node.X, $Node.Y + $Node.H - [int]($ellipseHeight / 2))
    $Graphics.DrawLine($pen, $Node.X + $Node.W, $Node.Y + [int]($ellipseHeight / 2), $Node.X + $Node.W, $Node.Y + $Node.H - [int]($ellipseHeight / 2))
    $Graphics.DrawEllipse($pen, $Node.X, $Node.Y + $Node.H - $ellipseHeight, $Node.W, $ellipseHeight)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 10) ($Node.W - 20) ($Node.H - 20) $Node.FontSize $Node.FontStyle $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Boundary($Graphics, $Node) {
    $path = New-RoundedPath $Node.X $Node.Y $Node.W $Node.H 18
    $pen = New-Pen $Node.Stroke 2.2 $false $true
    $Graphics.DrawPath($pen, $path)
    Draw-Label $Graphics $Node.Label ($Node.X + 16) ($Node.Y + 8) ($Node.W - 32) 24 $Node.FontSize $Node.FontStyle $Node.Stroke 'Near' 'Near'
    $pen.Dispose()
    $path.Dispose()
}

function Get-NodeMap($Nodes) {
    $map = @{}
    foreach ($node in $Nodes) {
        $map[$node.Id] = $node
    }
    return $map
}

function Get-AnchorPoint($Node, $TargetNode) {
    $cx = $Node.X + ($Node.W / 2.0)
    $cy = $Node.Y + ($Node.H / 2.0)
    $tx = $TargetNode.X + ($TargetNode.W / 2.0)
    $ty = $TargetNode.Y + ($TargetNode.H / 2.0)
    $dx = $tx - $cx
    $dy = $ty - $cy
    if ([Math]::Abs($dx) -ge [Math]::Abs($dy)) {
        if ($dx -ge 0) {
            return [System.Drawing.PointF]::new([single]($Node.X + $Node.W), [single]$cy)
        }
        return [System.Drawing.PointF]::new([single]$Node.X, [single]$cy)
    }
    if ($dy -ge 0) {
        return [System.Drawing.PointF]::new([single]$cx, [single]($Node.Y + $Node.H))
    }
    return [System.Drawing.PointF]::new([single]$cx, [single]$Node.Y)
}

function Get-Side($Node, $TargetNode) {
    $cx = $Node.X + ($Node.W / 2.0)
    $cy = $Node.Y + ($Node.H / 2.0)
    $tx = $TargetNode.X + ($TargetNode.W / 2.0)
    $ty = $TargetNode.Y + ($TargetNode.H / 2.0)
    $dx = $tx - $cx
    $dy = $ty - $cy
    if ([Math]::Abs($dx) -ge [Math]::Abs($dy)) {
        if ($dx -ge 0) { return 'right' }
        return 'left'
    }
    if ($dy -ge 0) { return 'bottom' }
    return 'top'
}

function Get-SlotRatio([int]$Slot, [int]$Total) {
    if ($Total -le 1) { return 0.5 }
    return $Slot / ($Total + 1.0)
}

function Get-DistributedAnchorPoint($Node, [string]$Side, [int]$Slot, [int]$Total) {
    $ratio = Get-SlotRatio $Slot $Total
    switch ($Side) {
        'top' {
            return [System.Drawing.PointF]::new([single]($Node.X + ($Node.W * $ratio)), [single]$Node.Y)
        }
        'bottom' {
            return [System.Drawing.PointF]::new([single]($Node.X + ($Node.W * $ratio)), [single]($Node.Y + $Node.H))
        }
        'left' {
            return [System.Drawing.PointF]::new([single]$Node.X, [single]($Node.Y + ($Node.H * $ratio)))
        }
        default {
            return [System.Drawing.PointF]::new([single]($Node.X + $Node.W), [single]($Node.Y + ($Node.H * $ratio)))
        }
    }
}

function Get-AnchorStyle([string]$Prefix, [string]$Side, [int]$Slot, [int]$Total) {
    $ratio = [Math]::Round((Get-SlotRatio $Slot $Total), 3)
    switch ($Side) {
        'top' {
            return "$($Prefix)X=$ratio;$($Prefix)Y=0;$($Prefix)Dx=0;$($Prefix)Dy=0;"
        }
        'bottom' {
            return "$($Prefix)X=$ratio;$($Prefix)Y=1;$($Prefix)Dx=0;$($Prefix)Dy=0;"
        }
        'left' {
            return "$($Prefix)X=0;$($Prefix)Y=$ratio;$($Prefix)Dx=0;$($Prefix)Dy=0;"
        }
        default {
            return "$($Prefix)X=1;$($Prefix)Y=$ratio;$($Prefix)Dx=0;$($Prefix)Dy=0;"
        }
    }
}

function Apply-EdgeLayout($Diagram) {
    $map = Get-NodeMap $Diagram.Nodes
    $sourceGroups = @{}
    $targetGroups = @{}

    foreach ($edge in $Diagram.Edges) {
        $source = $map[$edge.Source]
        $target = $map[$edge.Target]
        if (-not $source -or -not $target) { continue }

        $sourceSide = Get-Side $source $target
        $targetSide = Get-Side $target $source

        $edge | Add-Member -NotePropertyName SourceSide -NotePropertyValue $sourceSide -Force
        $edge | Add-Member -NotePropertyName TargetSide -NotePropertyValue $targetSide -Force

        $sourceKey = "$($edge.Source)|$sourceSide"
        $targetKey = "$($edge.Target)|$targetSide"
        if (-not $sourceGroups.ContainsKey($sourceKey)) { $sourceGroups[$sourceKey] = @() }
        if (-not $targetGroups.ContainsKey($targetKey)) { $targetGroups[$targetKey] = @() }
        $sourceGroups[$sourceKey] += $edge
        $targetGroups[$targetKey] += $edge
    }

    foreach ($group in $sourceGroups.GetEnumerator()) {
        $slot = 1
        foreach ($edge in $group.Value) {
            $edge | Add-Member -NotePropertyName SourceSlot -NotePropertyValue $slot -Force
            $edge | Add-Member -NotePropertyName SourceSlotCount -NotePropertyValue $group.Value.Count -Force
            $slot++
        }
    }

    foreach ($group in $targetGroups.GetEnumerator()) {
        $slot = 1
        foreach ($edge in $group.Value) {
            $edge | Add-Member -NotePropertyName TargetSlot -NotePropertyValue $slot -Force
            $edge | Add-Member -NotePropertyName TargetSlotCount -NotePropertyValue $group.Value.Count -Force
            $slot++
        }
    }
}

function Draw-Edge($Graphics, $Edge, $NodeMap) {
    $source = $NodeMap[$Edge.Source]
    $target = $NodeMap[$Edge.Target]
    if (-not $source -or -not $target) { return }
    $sourceSide = if ($Edge.PSObject.Properties['SourceSide']) { $Edge.SourceSide } else { Get-Side $source $target }
    $targetSide = if ($Edge.PSObject.Properties['TargetSide']) { $Edge.TargetSide } else { Get-Side $target $source }
    $sourceSlot = if ($Edge.PSObject.Properties['SourceSlot']) { [int]$Edge.SourceSlot } else { 1 }
    $sourceSlotCount = if ($Edge.PSObject.Properties['SourceSlotCount']) { [int]$Edge.SourceSlotCount } else { 1 }
    $targetSlot = if ($Edge.PSObject.Properties['TargetSlot']) { [int]$Edge.TargetSlot } else { 1 }
    $targetSlotCount = if ($Edge.PSObject.Properties['TargetSlotCount']) { [int]$Edge.TargetSlotCount } else { 1 }
    $p1 = Get-DistributedAnchorPoint $source $sourceSide $sourceSlot $sourceSlotCount
    $p2 = Get-DistributedAnchorPoint $target $targetSide $targetSlot $targetSlotCount
    $stroke = if ($Edge.Stroke) { $Edge.Stroke } else { '#7a7a7a' }
    $pen = New-Pen $stroke 2.1 $Edge.Arrow $Edge.Dashed
    $midX = [single](($p1.X + $p2.X) / 2)
    $midY = [single](($p1.Y + $p2.Y) / 2)
    if (($sourceSide -eq 'left' -or $sourceSide -eq 'right') -and ($targetSide -eq 'left' -or $targetSide -eq 'right')) {
        $pts = @(
            $p1,
            [System.Drawing.PointF]::new($midX, $p1.Y),
            [System.Drawing.PointF]::new($midX, $p2.Y),
            $p2
        )
        $Graphics.DrawLines($pen, $pts)
    } else {
        $pts = @(
            $p1,
            [System.Drawing.PointF]::new($p1.X, $midY),
            [System.Drawing.PointF]::new($p2.X, $midY),
            $p2
        )
        $Graphics.DrawLines($pen, $pts)
    }
    if ($Edge.Label) {
        $labelX = [int]$midX
        $labelY = [int]$midY
        $bg = New-Brush '#ffffff'
        $Graphics.FillRectangle($bg, $labelX - 52, $labelY - 13, 104, 26)
        $bg.Dispose()
        Draw-Label $Graphics $Edge.Label ($labelX - 50) ($labelY - 12) 100 24 12 'Bold' $Theme.Text
    }
    $pen.Dispose()
}

function Draw-Node($Graphics, $Node) {
    switch ($Node.Shape) {
        'round' { Draw-RoundRect $Graphics $Node }
        'rect' { Draw-Rect $Graphics $Node }
        'ellipse' { Draw-Ellipse $Graphics $Node }
        'diamond' { Draw-Diamond $Graphics $Node }
        'parallelogram' { Draw-Parallelogram $Graphics $Node }
        'cylinder' { Draw-Cylinder $Graphics $Node }
        'boundary' { Draw-Boundary $Graphics $Node }
        default { Draw-Rect $Graphics $Node }
    }
}

function Save-Png($Diagram, [string]$BasePath) {
    Apply-EdgeLayout $Diagram
    $canvas = New-Canvas $Diagram.Width $Diagram.Height
    $g = $canvas.Graphics
    Draw-Grid $g $Diagram.Width $Diagram.Height
    $map = Get-NodeMap $Diagram.Nodes
    foreach ($edge in $Diagram.Edges) {
        Draw-Edge $g $edge $map
    }
    foreach ($node in $Diagram.Nodes) {
        Draw-Node $g $node
    }
    Draw-Label $g $Diagram.Title 340 18 ($Diagram.Width - 680) 34 24 'Bold' $Theme.Text
    $canvas.Bitmap.Save("$BasePath.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $canvas.Bitmap.Dispose()
}

function New-Node {
    param(
        [string]$Id,
        [string]$Shape,
        [string]$Label,
        [int]$X,
        [int]$Y,
        [int]$W,
        [int]$H,
        [string]$Fill,
        [string]$Stroke,
        [int]$FontSize = 18,
        [string]$FontStyle = 'Regular'
    )
    return [pscustomobject]@{
        Id = $Id
        Shape = $Shape
        Label = $Label
        X = $X
        Y = $Y
        W = $W
        H = $H
        Fill = $Fill
        Stroke = $Stroke
        FontSize = $FontSize
        FontStyle = $FontStyle
    }
}

function New-Edge {
    param(
        [string]$Source,
        [string]$Target,
        [string]$Label = '',
        [bool]$Arrow = $true,
        [bool]$Dashed = $false,
        [string]$Stroke = $null
    )
    return [pscustomobject]@{
        Source = $Source
        Target = $Target
        Label = $Label
        Arrow = $Arrow
        Dashed = $Dashed
        Stroke = $Stroke
    }
}

function Get-NodeStyle($Node) {
    $fontStyle = if ($Node.FontStyle -eq 'Bold') { 'fontStyle=1;' } else { '' }
    switch ($Node.Shape) {
        'round' {
            return "rounded=1;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'rect' {
            return "rounded=0;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'ellipse' {
            return "ellipse;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'diamond' {
            return "rhombus;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'parallelogram' {
            return "shape=parallelogram;perimeter=parallelogramPerimeter;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'cylinder' {
            return "shape=cylinder3;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
        'boundary' {
            return "rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=8 8;fillColor=none;strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Node.Stroke);align=left;verticalAlign=top;spacingLeft=12;spacingTop=8;"
        }
        default {
            return "rounded=1;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$($Node.FontSize);$fontStyle fontColor=$($Theme.Text);"
        }
    }
}

function Save-Drawio($Diagram, [string]$BasePath) {
    Apply-EdgeLayout $Diagram
    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine('<mxfile host="drawio" version="26.0.0">')
    [void]$sb.AppendLine('  <diagram name="Page-1">')
    [void]$sb.AppendLine("    <mxGraphModel page=`"1`" pageWidth=`"$($Diagram.Width)`" pageHeight=`"$($Diagram.Height)`" grid=`"1`" gridSize=`"10`">")
    [void]$sb.AppendLine('      <root>')
    [void]$sb.AppendLine('        <mxCell id="0" />')
    [void]$sb.AppendLine('        <mxCell id="1" parent="0" />')
    [void]$sb.AppendLine("        <mxCell id=`"title`" value=`"$(Xml-Escape $Diagram.Title)`" style=`"text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontColor=$($Theme.Text);`" vertex=`"1`" parent=`"1`">")
    [void]$sb.AppendLine("          <mxGeometry x=`"340`" y=`"20`" width=`"$([Math]::Max(600, $Diagram.Width - 680))`" height=`"34`" as=`"geometry`" />")
    [void]$sb.AppendLine('        </mxCell>')

    foreach ($node in $Diagram.Nodes) {
        $label = Xml-Escape $node.Label
        $style = Get-NodeStyle $node
        [void]$sb.AppendLine("        <mxCell id=`"$($node.Id)`" value=`"$label`" style=`"$style`" vertex=`"1`" parent=`"1`">")
        [void]$sb.AppendLine("          <mxGeometry x=`"$($node.X)`" y=`"$($node.Y)`" width=`"$($node.W)`" height=`"$($node.H)`" as=`"geometry`" />")
        [void]$sb.AppendLine('        </mxCell>')
    }

    $edgeId = 1
    foreach ($edge in $Diagram.Edges) {
        $label = Xml-Escape $edge.Label
        $dashed = if ($edge.Dashed) { 'dashed=1;' } else { '' }
        $arrow = if ($edge.Arrow) { 'endArrow=block;endFill=1;' } else { 'endArrow=none;' }
        $stroke = if ($edge.Stroke) { $edge.Stroke } else { '#7a7a7a' }
        $sourceAnchor = Get-AnchorStyle 'exit' $edge.SourceSide $edge.SourceSlot $edge.SourceSlotCount
        $targetAnchor = Get-AnchorStyle 'entry' $edge.TargetSide $edge.TargetSlot $edge.TargetSlotCount
        [void]$sb.AppendLine("        <mxCell id=`"edge$edgeId`" value=`"$label`" style=`"edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=$stroke;strokeWidth=2;$dashed$arrow$sourceAnchor$targetAnchor fontSize=12;fontColor=$($Theme.Text);`" edge=`"1`" parent=`"1`" source=`"$($edge.Source)`" target=`"$($edge.Target)`">")
        [void]$sb.AppendLine('          <mxGeometry relative="1" as="geometry" />')
        [void]$sb.AppendLine('        </mxCell>')
        $edgeId++
    }

    [void]$sb.AppendLine('      </root>')
    [void]$sb.AppendLine('    </mxGraphModel>')
    [void]$sb.AppendLine('  </diagram>')
    [void]$sb.AppendLine('</mxfile>')
    [System.IO.File]::WriteAllText("$BasePath.drawio", $sb.ToString(), [System.Text.Encoding]::UTF8)
}

function Get-DrawioExe {
    $cmd = Get-Command draw.io -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    $cmdExe = Get-Command draw.io.exe -ErrorAction SilentlyContinue
    if ($cmdExe) {
        return $cmdExe.Source
    }
    throw 'draw.io CLI not found'
}

function Export-Drawio([string]$DrawioPath) {
    $drawio = Get-DrawioExe
    $baseDir = Split-Path -Parent $DrawioPath
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($DrawioPath)
    & $drawio -x -f png -o "$baseName.png" $DrawioPath | Out-Null
    & $drawio -x -f svg -o "$baseName.svg" $DrawioPath | Out-Null
}

Ensure-Dir $OutputDir

$Diagrams = @(
    @{
        File = '01-系统总体架构图'
        Title = '系统总体架构图'
        Width = 1800
        Height = 1260
        Nodes = @(
            (New-Node 'n1' 'boundary' '表现层' 80 120 1640 150 '#ffffff' $Theme.GreyStroke 18 'Bold'),
            (New-Node 'n2' 'boundary' '接入与安全层' 80 310 1640 130 '#ffffff' $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'n3' 'boundary' '业务服务层' 80 470 1640 200 '#ffffff' $Theme.GreenStroke 18 'Bold'),
            (New-Node 'n4' 'boundary' '数据与外部能力层' 80 720 1640 420 '#ffffff' $Theme.BlueStroke 18 'Bold'),
            (New-Node 'browser' 'round' '浏览器客户端' 220 165 240 70 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'frontend' 'round' 'Vue 3 前端`n路由 / 页面 / 状态管理' 720 155 360 90 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'jwt' 'round' 'JWT 拦截器' 440 340 240 70 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'role' 'round' '角色拦截器' 770 340 240 70 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'controller' 'round' '控制器层`nAuth / Chat / Rag / Upload / CRUD / AI Query' 470 520 520 90 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'service' 'round' '服务层`n认证服务 / 教务服务 / 聊天服务 / 知识库服务 / OCR 服务 / AI 查询服务' 360 625 740 90 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'mapper' 'round' '数据访问层`nMyBatis Mapper + JDBC Repository' 580 760 300 80 $Theme.YellowFill $Theme.YellowStroke 18 'Bold'),
            (New-Node 'mysql' 'cylinder' 'MySQL`n业务数据 / 会话数据 / 文档元数据' 150 930 270 90 $Theme.GreyFill $Theme.GreyStroke 17 'Bold'),
            (New-Node 'redis' 'cylinder' 'Redis Vector Store`n知识库向量索引' 470 930 250 90 $Theme.GreyFill $Theme.GreyStroke 17 'Bold'),
            (New-Node 'oss' 'cylinder' '阿里云 OSS`n图片与原始文件存储' 760 930 250 90 $Theme.GreyFill $Theme.GreyStroke 17 'Bold'),
            (New-Node 'ocr' 'round' 'Python OCR 服务`nPDF / 图片 / 文档解析' 1060 925 250 100 $Theme.RedFill $Theme.RedStroke 17 'Bold'),
            (New-Node 'llm' 'round' '外部大模型平台`n通义 / GLM / GPT / Kimi / DeepSeek' 1360 915 280 110 $Theme.RedFill $Theme.RedStroke 17 'Bold'),
            (New-Node 'searxng' 'round' 'SearXNG 搜索服务`n联网搜索结果获取' 1360 1060 280 80 $Theme.RedFill $Theme.RedStroke 17 'Bold')
        )
        Edges = @(
            (New-Edge 'browser' 'frontend' '页面访问'),
            (New-Edge 'frontend' 'jwt' 'HTTP / SSE'),
            (New-Edge 'jwt' 'role'),
            (New-Edge 'role' 'controller'),
            (New-Edge 'controller' 'service'),
            (New-Edge 'service' 'mapper'),
            (New-Edge 'mapper' 'mysql'),
            (New-Edge 'service' 'redis'),
            (New-Edge 'service' 'oss'),
            (New-Edge 'service' 'ocr'),
            (New-Edge 'service' 'llm'),
            (New-Edge 'service' 'searxng')
        )
    },
    @{
        File = '02-系统部署图'
        Title = '系统部署图'
        Width = 1800
        Height = 1140
        Nodes = @(
            (New-Node 'zone1' 'boundary' '客户端环境' 70 140 300 250 '#ffffff' $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'zone2' 'boundary' '应用部署区' 430 100 520 390 '#ffffff' $Theme.BlueStroke 18 'Bold'),
            (New-Node 'zone3' 'boundary' '内部服务区' 1010 100 350 480 '#ffffff' $Theme.GreenStroke 18 'Bold'),
            (New-Node 'zone4' 'boundary' '外部云服务区' 1420 100 310 520 '#ffffff' $Theme.RedStroke 18 'Bold'),
            (New-Node 'client' 'round' '用户浏览器' 120 230 200 80 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'fe' 'round' '前端静态页面`nVue 3 / Vite 构建产物' 560 180 260 90 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'be' 'round' 'Spring Boot 后端`nREST API / SSE / 业务编排' 560 330 260 100 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'mysql2' 'cylinder' 'MySQL' 1070 180 220 80 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'redis2' 'cylinder' 'Redis Vector Store' 1070 310 220 80 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'ocr2' 'round' 'Python OCR 服务' 1070 440 220 80 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'oss2' 'round' '阿里云 OSS' 1470 180 210 80 $Theme.RedFill $Theme.RedStroke 18 'Bold'),
            (New-Node 'llm2' 'round' '大模型平台' 1470 320 210 80 $Theme.RedFill $Theme.RedStroke 18 'Bold'),
            (New-Node 'search2' 'round' 'SearXNG 服务' 1470 460 210 80 $Theme.RedFill $Theme.RedStroke 18 'Bold')
        )
        Edges = @(
            (New-Edge 'client' 'fe' 'HTTPS'),
            (New-Edge 'fe' 'be' 'API 调用'),
            (New-Edge 'be' 'mysql2' 'JDBC'),
            (New-Edge 'be' 'redis2' 'Redis'),
            (New-Edge 'be' 'ocr2' 'HTTP'),
            (New-Edge 'be' 'oss2' 'OSS API'),
            (New-Edge 'be' 'llm2' 'OpenAI Compatible API'),
            (New-Edge 'be' 'search2' 'HTTP Search API')
        )
    },
    @{
        File = '03-系统功能结构图'
        Title = '系统功能结构图'
        Width = 1700
        Height = 1280
        Nodes = @(
            (New-Node 'root' 'round' '校园知识问答助手系统' 650 90 400 80 $Theme.OrangeFill $Theme.OrangeStroke 24 'Bold'),
            (New-Node 'basic' 'round' '基础业务模块' 300 250 300 80 $Theme.BlueFill $Theme.BlueStroke 20 'Bold'),
            (New-Node 'ai' 'round' '智能扩展模块' 1100 250 300 80 $Theme.GreenFill $Theme.GreenStroke 20 'Bold'),
            (New-Node 'auth3' 'round' '用户认证模块' 340 420 220 70 $Theme.PurpleFill $Theme.PurpleStroke 17 'Bold'),
            (New-Node 'user3' 'round' '用户管理模块' 340 530 220 70 $Theme.PurpleFill $Theme.PurpleStroke 17 'Bold'),
            (New-Node 'student3' 'round' '学生管理模块' 340 640 220 70 $Theme.YellowFill $Theme.YellowStroke 17 'Bold'),
            (New-Node 'teacher3' 'round' '教师管理模块' 340 750 220 70 $Theme.YellowFill $Theme.YellowStroke 17 'Bold'),
            (New-Node 'course3' 'round' '课程管理模块' 340 860 220 70 $Theme.YellowFill $Theme.YellowStroke 17 'Bold'),
            (New-Node 'grade3' 'round' '成绩管理模块' 340 970 220 70 $Theme.YellowFill $Theme.YellowStroke 17 'Bold'),
            (New-Node 'chat3' 'round' 'AI 对话模块' 1140 390 220 70 $Theme.BlueFill $Theme.BlueStroke 17 'Bold'),
            (New-Node 'rag3' 'round' 'RAG 知识库模块' 1140 500 220 70 $Theme.BlueFill $Theme.BlueStroke 17 'Bold'),
            (New-Node 'ocr3' 'round' 'OCR 文档解析模块' 1140 610 220 70 $Theme.GreenFill $Theme.GreenStroke 17 'Bold'),
            (New-Node 'upload3' 'round' '文件上传模块' 1140 720 220 70 $Theme.GreenFill $Theme.GreenStroke 17 'Bold'),
            (New-Node 'query3' 'round' 'AI 数据查询模块' 1140 830 220 70 $Theme.RedFill $Theme.RedStroke 17 'Bold'),
            (New-Node 'history3' 'round' '会话历史与日志模块' 1140 940 220 70 $Theme.RedFill $Theme.RedStroke 17 'Bold')
        )
        Edges = @(
            (New-Edge 'root' 'basic'),
            (New-Edge 'root' 'ai'),
            (New-Edge 'basic' 'auth3'),
            (New-Edge 'auth3' 'user3'),
            (New-Edge 'user3' 'student3'),
            (New-Edge 'student3' 'teacher3'),
            (New-Edge 'teacher3' 'course3'),
            (New-Edge 'course3' 'grade3'),
            (New-Edge 'ai' 'chat3'),
            (New-Edge 'chat3' 'rag3'),
            (New-Edge 'rag3' 'ocr3'),
            (New-Edge 'ocr3' 'upload3'),
            (New-Edge 'upload3' 'query3'),
            (New-Edge 'query3' 'history3')
        )
    },
    @{
        File = '04-系统业务总流程图'
        Title = '系统业务总流程图'
        Width = 2100
        Height = 1560
        Nodes = @(
            (New-Node 'start' 'ellipse' '开始' 930 80 220 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold'),
            (New-Node 'login' 'parallelogram' '用户注册 / 登录' 860 200 360 80 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'home' 'rect' '进入系统首页并根据角色展示菜单' 840 320 400 80 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'select' 'diamond' '选择业务场景' 910 450 260 120 $Theme.YellowFill $Theme.YellowStroke 18 'Bold'),
            (New-Node 'crud' 'rect' "教务管理`n用户 / 学生 / 教师 / 课程 / 成绩" 120 680 360 100 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'crud2' 'rect' '执行查询、录入、修改、删除' 120 840 360 90 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'chat' 'rect' "进入 AI 对话`n选择模型与模式" 820 640 440 90 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'direct' 'rect' '模式一：普通直接对话' 540 830 240 80 $Theme.BlueFill $Theme.BlueStroke 17 'Bold'),
            (New-Node 'ragMode' 'rect' '模式二：RAG 知识库问答' 820 830 240 80 $Theme.BlueFill $Theme.BlueStroke 17 'Bold'),
            (New-Node 'searchMode' 'rect' '模式三：联网搜索问答' 1100 830 240 80 $Theme.BlueFill $Theme.BlueStroke 17 'Bold'),
            (New-Node 'sqlMode' 'rect' '模式四：AI 数据查询' 1380 830 240 80 $Theme.RedFill $Theme.RedStroke 17 'Bold'),
            (New-Node 'chat2' 'rect' '按选定模式组装上下文并调用模型' 820 980 520 90 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'chat3' 'rect' '返回结果并保存会话历史 / 查询日志' 860 1130 440 90 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'rag' 'rect' '知识库管理：上传文档' 1620 650 320 80 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'rag2' 'rect' '解析或 OCR 预览并人工确认' 1590 790 380 90 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'rag3' 'rect' '写入 OSS、数据库与向量索引' 1590 930 380 90 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'end' 'ellipse' '结束' 940 1360 200 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold')
        )
        Edges = @(
            (New-Edge 'start' 'login'),
            (New-Edge 'login' 'home'),
            (New-Edge 'home' 'select'),
            (New-Edge 'select' 'crud' '教务管理'),
            (New-Edge 'crud' 'crud2'),
            (New-Edge 'crud2' 'end'),
            (New-Edge 'select' 'chat' 'AI 功能'),
            (New-Edge 'chat' 'direct'),
            (New-Edge 'chat' 'ragMode'),
            (New-Edge 'chat' 'searchMode'),
            (New-Edge 'chat' 'sqlMode'),
            (New-Edge 'direct' 'chat2'),
            (New-Edge 'ragMode' 'chat2'),
            (New-Edge 'searchMode' 'chat2'),
            (New-Edge 'sqlMode' 'chat2'),
            (New-Edge 'chat2' 'chat3'),
            (New-Edge 'chat3' 'end'),
            (New-Edge 'select' 'rag' '知识库管理'),
            (New-Edge 'rag' 'rag2'),
            (New-Edge 'rag2' 'rag3'),
            (New-Edge 'rag3' 'ragMode' '供 RAG 问答使用'),
            (New-Edge 'rag3' 'end' '入库完成')
        )
    }
)

foreach ($diagram in $Diagrams) {
    $basePath = Join-Path $OutputDir $diagram.File
    Save-Drawio $diagram $basePath
    Save-Png $diagram $basePath
    try {
        Export-Drawio "$basePath.drawio"
    } catch {
        Write-Warning "draw.io export failed for $($diagram.File), kept .drawio and local .png fallback."
    }
}

param(
    [string]$OutputRoot = (Join-Path (Get-Location) '毕设文档\picture\系统介绍图集')
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
    Grid = '#f1f3f5'
    Edge = '#7a7a7a'
}

function Ensure-Dir([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Xml-Escape([string]$Text) {
    if ($null -eq $Text) { return '' }
    return $Text.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;').Replace('"', '&quot;').Replace("`r", '').Replace("`n", '&#xa;')
}

function Value-OrDefault($Value, $Fallback) {
    if ($null -eq $Value -or $Value -eq '') { return $Fallback }
    return $Value
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

function Draw-Grid($Graphics, [int]$Width, [int]$Height) {
    $pen = New-Object System.Drawing.Pen (Color $Theme.Grid), 1
    for ($x = 0; $x -le $Width; $x += 40) {
        $Graphics.DrawLine($pen, $x, 0, $x, $Height)
    }
    for ($y = 0; $y -le $Height; $y += 40) {
        $Graphics.DrawLine($pen, 0, $y, $Width, $y)
    }
    $pen.Dispose()
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
    $Graphics.DrawString($Text, $font, $brush, $rect, $fmt)
    $fmt.Dispose()
    $font.Dispose()
    $brush.Dispose()
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
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillPath($fill, $path)
    $Graphics.DrawPath($pen, $path)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 8) ($Node.W - 20) ($Node.H - 16) (Value-OrDefault $Node.FontSize 18) (Value-OrDefault $Node.FontStyle 'Regular') $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
    $path.Dispose()
}

function Draw-Rect($Graphics, $Node) {
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillRectangle($fill, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.DrawRectangle($pen, $Node.X, $Node.Y, $Node.W, $Node.H)
    Draw-Label $Graphics $Node.Label ($Node.X + 8) ($Node.Y + 8) ($Node.W - 16) ($Node.H - 16) (Value-OrDefault $Node.FontSize 18) (Value-OrDefault $Node.FontStyle 'Regular') $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Ellipse($Graphics, $Node) {
    $fill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillEllipse($fill, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.DrawEllipse($pen, $Node.X, $Node.Y, $Node.W, $Node.H)
    Draw-Label $Graphics $Node.Label ($Node.X + 10) ($Node.Y + 8) ($Node.W - 20) ($Node.H - 16) (Value-OrDefault $Node.FontSize 18) (Value-OrDefault $Node.FontStyle 'Regular') $Theme.Text
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
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillPolygon($fill, $pts)
    $Graphics.DrawPolygon($pen, $pts)
    Draw-Label $Graphics $Node.Label ($Node.X + 15) ($Node.Y + 12) ($Node.W - 30) ($Node.H - 24) (Value-OrDefault $Node.FontSize 16) (Value-OrDefault $Node.FontStyle 'Bold') $Theme.Text
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
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillPolygon($fill, $pts)
    $Graphics.DrawPolygon($pen, $pts)
    Draw-Label $Graphics $Node.Label ($Node.X + 12) ($Node.Y + 10) ($Node.W - 24) ($Node.H - 20) (Value-OrDefault $Node.FontSize 17) (Value-OrDefault $Node.FontStyle 'Regular') $Theme.Text
    $pen.Dispose()
    $fill.Dispose()
}

function Draw-Actor($Graphics, $Node) {
    $pen = New-Pen $Node.Stroke 3
    $labelFont = Value-OrDefault $Node.FontSize 18
    $cx = $Node.X + [int]($Node.W / 2)
    $headSize = [int]([Math]::Min($Node.W * 0.32, 34))
    $bodyTop = $Node.Y + $headSize + 6
    $bodyBottom = $bodyTop + 48
    $armY = $bodyTop + 12
    $legY = $bodyBottom + 36
    $Graphics.DrawEllipse($pen, $cx - [int]($headSize / 2), $Node.Y, $headSize, $headSize)
    $Graphics.DrawLine($pen, $cx, $Node.Y + $headSize, $cx, $bodyBottom)
    $Graphics.DrawLine($pen, $cx - 28, $armY, $cx + 28, $armY)
    $Graphics.DrawLine($pen, $cx, $bodyBottom, $cx - 24, $legY)
    $Graphics.DrawLine($pen, $cx, $bodyBottom, $cx + 24, $legY)
    Draw-Label $Graphics $Node.Label ($Node.X - 10) ($legY + 10) ($Node.W + 20) 36 $labelFont 'Regular' $Theme.Text
    $pen.Dispose()
}

function Draw-Table($Graphics, $Node) {
    $headerHeight = [int]([Math]::Min(38, [Math]::Max(32, $Node.H * 0.18)))
    $fill = New-Brush '#ffffff'
    $headerFill = New-Brush $Node.Fill
    $pen = New-Pen $Node.Stroke 2.5
    $Graphics.FillRectangle($fill, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.FillRectangle($headerFill, $Node.X, $Node.Y, $Node.W, $headerHeight)
    $Graphics.DrawRectangle($pen, $Node.X, $Node.Y, $Node.W, $Node.H)
    $Graphics.DrawLine($pen, $Node.X, $Node.Y + $headerHeight, $Node.X + $Node.W, $Node.Y + $headerHeight)
    Draw-Label $Graphics $Node.Label ($Node.X + 8) ($Node.Y + 2) ($Node.W - 16) ($headerHeight - 4) 17 'Bold' '#1f2328'
    $bodyText = ($Node.Fields -join "`n")
    Draw-Label $Graphics $bodyText ($Node.X + 10) ($Node.Y + $headerHeight + 6) ($Node.W - 20) ($Node.H - $headerHeight - 12) 12 'Regular' $Theme.Text 'Near' 'Near'
    $fill.Dispose()
    $headerFill.Dispose()
    $pen.Dispose()
}

function Draw-Boundary($Graphics, $Boundary) {
    if (-not $Boundary) { return }
    $path = New-RoundedPath $Boundary.X $Boundary.Y $Boundary.W $Boundary.H 18
    $pen = New-Pen $Boundary.Stroke 2.5 $false $true
    $Graphics.DrawPath($pen, $path)
    Draw-Label $Graphics $Boundary.Label ($Boundary.X + 18) ($Boundary.Y + 8) ($Boundary.W - 36) 26 18 'Bold' $Boundary.Stroke
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

function Draw-Edge($Graphics, $Edge, $NodeMap) {
    $source = $NodeMap[$Edge.Source]
    $target = $NodeMap[$Edge.Target]
    if (-not $source -or -not $target) { return }
    $p1 = Get-AnchorPoint $source $target
    $p2 = Get-AnchorPoint $target $source
    $pen = New-Pen (Value-OrDefault $Edge.Stroke $Theme.Edge) (Value-OrDefault $Edge.Width 2.2) ($Edge.Arrow -ne $false) ($Edge.Dashed -eq $true)
    $Graphics.DrawLine($pen, $p1, $p2)
    if ($Edge.Label) {
        $midX = [int](($p1.X + $p2.X) / 2)
        $midY = [int](($p1.Y + $p2.Y) / 2)
        $font = New-Font 12 'Bold'
        $size = $Graphics.MeasureString($Edge.Label, $font)
        $labelW = [int]([Math]::Ceiling($size.Width) + 18)
        $labelH = [int]([Math]::Ceiling($size.Height) + 8)
        $bg = New-Brush '#ffffff'
        $Graphics.FillRectangle($bg, $midX - [int]($labelW / 2), $midY - [int]($labelH / 2), $labelW, $labelH)
        $bg.Dispose()
        Draw-Label $Graphics $Edge.Label ($midX - [int]($labelW / 2)) ($midY - [int]($labelH / 2)) $labelW $labelH 12 'Bold' $Theme.Text
        $font.Dispose()
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
        'actor' { Draw-Actor $Graphics $Node }
        'table' { Draw-Table $Graphics $Node }
        default { Draw-Rect $Graphics $Node }
    }
}

function Get-DrawioStyle($Node) {
    switch ($Node.Shape) {
        'round' { return "rounded=1;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 18);fontColor=$($Theme.Text);" }
        'rect' { return "rounded=0;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 18);fontColor=$($Theme.Text);" }
        'ellipse' { return "ellipse;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 18);fontColor=$($Theme.Text);" }
        'diamond' { return "rhombus;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 16);fontStyle=1;fontColor=$($Theme.Text);" }
        'parallelogram' { return "shape=parallelogram;perimeter=parallelogramPerimeter;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 17);fontColor=$($Theme.Text);" }
        'actor' { return "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;strokeColor=$($Node.Stroke);fontSize=$(Value-OrDefault $Node.FontSize 18);fontColor=$($Theme.Text);" }
        'table' { return "rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=$($Node.Stroke);fontSize=12;align=left;spacingLeft=8;spacingTop=8;verticalAlign=top;fontColor=$($Theme.Text);" }
        default { return "rounded=0;whiteSpace=wrap;html=1;fillColor=$($Node.Fill);strokeColor=$($Node.Stroke);fontSize=18;fontColor=$($Theme.Text);" }
    }
}

function Get-DrawioLabel($Node) {
    if ($Node.Shape -eq 'table') {
        return "$($Node.Label)`n$('-' * 20)`n$([string]::Join("`n", $Node.Fields))"
    }
    return $Node.Label
}

function Save-Drawio($Diagram, [string]$BasePath) {
    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine('<mxfile host="drawio" version="26.0.0">')
    [void]$sb.AppendLine('  <diagram name="Page-1">')
    [void]$sb.AppendLine("    <mxGraphModel page=`"1`" pageWidth=`"$($Diagram.Width)`" pageHeight=`"$($Diagram.Height)`" grid=`"1`" gridSize=`"10`">")
    [void]$sb.AppendLine('      <root>')
    [void]$sb.AppendLine('        <mxCell id="0" />')
    [void]$sb.AppendLine('        <mxCell id="1" parent="0" />')

    if ($Diagram.Boundary) {
        $boundary = $Diagram.Boundary
        [void]$sb.AppendLine("        <mxCell id=`"boundary`" value=`"$(Xml-Escape $boundary.Label)`" style=`"rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=8 8;fillColor=none;strokeColor=$($boundary.Stroke);fontStyle=1;fontSize=18;fontColor=$($boundary.Stroke);align=left;spacingLeft=14;verticalAlign=top;spacingTop=8;`" vertex=`"1`" parent=`"1`">")
        [void]$sb.AppendLine("          <mxGeometry x=`"$($boundary.X)`" y=`"$($boundary.Y)`" width=`"$($boundary.W)`" height=`"$($boundary.H)`" as=`"geometry`" />")
        [void]$sb.AppendLine('        </mxCell>')
    }

    [void]$sb.AppendLine("        <mxCell id=`"title`" value=`"$(Xml-Escape $Diagram.Title)`" style=`"text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontColor=$($Theme.Text);`" vertex=`"1`" parent=`"1`">")
    [void]$sb.AppendLine("          <mxGeometry x=`"380`" y=`"20`" width=`"$([Math]::Max(400, $Diagram.Width - 760))`" height=`"30`" as=`"geometry`" />")
    [void]$sb.AppendLine('        </mxCell>')

    foreach ($node in $Diagram.Nodes) {
        $label = Xml-Escape (Get-DrawioLabel $node)
        $style = Get-DrawioStyle $node
        [void]$sb.AppendLine("        <mxCell id=`"$($node.Id)`" value=`"$label`" style=`"$style`" vertex=`"1`" parent=`"1`">")
        [void]$sb.AppendLine("          <mxGeometry x=`"$($node.X)`" y=`"$($node.Y)`" width=`"$($node.W)`" height=`"$($node.H)`" as=`"geometry`" />")
        [void]$sb.AppendLine('        </mxCell>')
    }

    $edgeIndex = 0
    foreach ($edge in $Diagram.Edges) {
        $edgeIndex++
        $label = Xml-Escape (Value-OrDefault $edge.Label '')
        $dashedPart = if ($edge.Dashed -eq $true) { 'dashed=1;' } else { '' }
        $arrowPart = if ($edge.Arrow -eq $false) { 'endArrow=none;' } else { 'endArrow=block;endFill=1;' }
        $stroke = Value-OrDefault $edge.Stroke $Theme.Edge
        [void]$sb.AppendLine("        <mxCell id=`"edge$edgeIndex`" value=`"$label`" style=`"edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=$stroke;strokeWidth=2;$dashedPart$arrowPart;fontSize=12;fontColor=$($Theme.Text);`" edge=`"1`" parent=`"1`" source=`"$($edge.Source)`" target=`"$($edge.Target)`">")
        [void]$sb.AppendLine('          <mxGeometry relative="1" as="geometry" />')
        [void]$sb.AppendLine('        </mxCell>')
    }

    [void]$sb.AppendLine('      </root>')
    [void]$sb.AppendLine('    </mxGraphModel>')
    [void]$sb.AppendLine('  </diagram>')
    [void]$sb.AppendLine('</mxfile>')
    [System.IO.File]::WriteAllText("$BasePath.drawio", $sb.ToString(), [System.Text.Encoding]::UTF8)
}

function Save-Png($Diagram, [string]$BasePath) {
    $canvas = New-Canvas $Diagram.Width $Diagram.Height
    $g = $canvas.Graphics
    Draw-Grid $g $Diagram.Width $Diagram.Height
    Draw-Boundary $g $Diagram.Boundary
    $map = Get-NodeMap $Diagram.Nodes
    foreach ($edge in $Diagram.Edges) {
        Draw-Edge $g $edge $map
    }
    foreach ($node in $Diagram.Nodes) {
        Draw-Node $g $node
    }
    Draw-Label $g $Diagram.Title 280 16 ($Diagram.Width - 560) 34 24 'Bold' $Theme.Text
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
        [string]$FontStyle = 'Regular',
        [string[]]$Fields = @()
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
        Fields = $Fields
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

$ThesisDir = Join-Path $OutputRoot '论文主图'
$SupplementDir = Join-Path $OutputRoot '技术补充图'
Ensure-Dir $OutputRoot
Ensure-Dir $ThesisDir
Ensure-Dir $SupplementDir

$Diagrams = @(
    @{
        Folder = $ThesisDir
        File = '01-系统总体功能结构图'
        Title = '系统总体功能结构图'
        Width = 1600
        Height = 1080
        Nodes = @(
            (New-Node 'root' 'round' '校园知识问答助手系统' 620 90 360 80 $Theme.OrangeFill $Theme.OrangeStroke 24 'Bold'),
            (New-Node 'auth' 'round' '登录认证与权限控制' 140 260 240 80 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'chat' 'round' '智能问答服务' 470 260 240 80 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'kb' 'round' '知识库管理' 800 260 240 80 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'edu' 'round' '教务数据管理' 1130 260 240 80 $Theme.YellowFill $Theme.YellowStroke 18 'Bold'),
            (New-Node 'audit' 'round' '日志审计与运维' 470 430 240 80 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'integrations' 'round' '外部服务集成' 800 430 240 80 $Theme.RedFill $Theme.RedStroke 18 'Bold'),
            (New-Node 'a1' 'rect' '注册 / 登录' 100 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'a2' 'rect' 'JWT 鉴权' 270 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'a3' 'rect' '角色控制' 185 700 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'b1' 'rect' '直接对话' 430 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'b2' 'rect' '知识库问答' 600 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'b3' 'rect' '联网搜索' 515 700 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'c1' 'rect' '文档上传' 770 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'c2' 'rect' 'OCR / 解析' 940 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'c3' 'rect' '向量检索' 855 700 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'd1' 'rect' '学生信息' 1110 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'd2' 'rect' '课程信息' 1280 610 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'd3' 'rect' '成绩信息' 1195 700 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'e1' 'rect' '操作日志' 430 860 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'e2' 'rect' '接口文档' 600 860 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'f1' 'rect' 'LLM 平台' 780 860 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'f2' 'rect' 'SearXNG' 950 860 150 64 $Theme.GreyFill $Theme.GreyStroke 16),
            (New-Node 'f3' 'rect' 'OSS / Redis / MySQL' 1120 860 220 64 $Theme.GreyFill $Theme.GreyStroke 16)
        )
        Edges = @(
            (New-Edge 'root' 'auth'),
            (New-Edge 'root' 'chat'),
            (New-Edge 'root' 'kb'),
            (New-Edge 'root' 'edu'),
            (New-Edge 'root' 'audit'),
            (New-Edge 'root' 'integrations'),
            (New-Edge 'auth' 'a1'),
            (New-Edge 'auth' 'a2'),
            (New-Edge 'auth' 'a3'),
            (New-Edge 'chat' 'b1'),
            (New-Edge 'chat' 'b2'),
            (New-Edge 'chat' 'b3'),
            (New-Edge 'kb' 'c1'),
            (New-Edge 'kb' 'c2'),
            (New-Edge 'kb' 'c3'),
            (New-Edge 'edu' 'd1'),
            (New-Edge 'edu' 'd2'),
            (New-Edge 'edu' 'd3'),
            (New-Edge 'audit' 'e1'),
            (New-Edge 'audit' 'e2'),
            (New-Edge 'integrations' 'f1'),
            (New-Edge 'integrations' 'f2'),
            (New-Edge 'integrations' 'f3')
        )
    },
    @{
        Folder = $ThesisDir
        File = '02-系统总用例图'
        Title = '系统总用例图'
        Width = 1780
        Height = 1260
        Boundary = @{
            X = 260; Y = 90; W = 1260; H = 1040; Label = '系统边界'; Stroke = $Theme.OrangeStroke
        }
        Nodes = @(
            (New-Node 'admin' 'actor' '管理员' 70 250 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'teacher' 'actor' '教师' 70 820 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'student' 'actor' '学生' 1600 530 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'baseCat' 'ellipse' '基础访问' 760 140 240 84 $Theme.PurpleFill $Theme.PurpleStroke 19 'Bold'),
            (New-Node 'aiCat' 'ellipse' 'AI 模块' 760 330 240 84 $Theme.BlueFill $Theme.BlueStroke 19 'Bold'),
            (New-Node 'adminCat' 'ellipse' '管理模块' 420 470 240 84 $Theme.OrangeFill $Theme.OrangeStroke 19 'Bold'),
            (New-Node 'teacherCat' 'ellipse' '教学模块' 760 840 240 84 $Theme.GreenFill $Theme.GreenStroke 19 'Bold'),
            (New-Node 'studentCat' 'ellipse' '查询模块' 1110 470 240 84 $Theme.YellowFill $Theme.YellowStroke 19 'Bold'),
            (New-Node 'login' 'ellipse' '登录系统' 760 240 240 72 '#ffffff' $Theme.BlueStroke 17),
            (New-Node 'chat' 'ellipse' '智能对话' 420 640 220 72 '#ffffff' $Theme.BlueStroke 17),
            (New-Node 'rag' 'ellipse' '知识库问答' 690 640 220 72 '#ffffff' $Theme.BlueStroke 17),
            (New-Node 'searchAi' 'ellipse' '联网搜索问答' 960 640 240 72 '#ffffff' $Theme.BlueStroke 17),
            (New-Node 'queryAi' 'ellipse' 'AI 教务数据查询' 1230 640 260 72 '#ffffff' $Theme.BlueStroke 17),
            (New-Node 'admin1' 'ellipse' '用户权限（增/改/查/删）' 350 980 300 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'admin2' 'ellipse' '档案管理（师/生，增/改/查/删）' 350 1070 340 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'admin3' 'ellipse' '课程成绩（增/改/查/删）' 350 1160 300 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'teacher1' 'ellipse' '学生信息（查/改）' 760 980 260 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'teacher2' 'ellipse' '课程成绩（增/改/查）' 760 1070 280 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'teacher3' 'ellipse' '私有知识库（上/改/删/查）' 760 1160 300 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'student1' 'ellipse' '课程信息（查）' 1090 980 240 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'student2' 'ellipse' '成绩信息（查）' 1090 1070 240 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'student3' 'ellipse' '个人信息（查）' 1090 1160 240 72 '#ffffff' $Theme.BlueStroke 16)
        )
        Edges = @(
            (New-Edge 'admin' 'baseCat' '' $false),
            (New-Edge 'teacher' 'baseCat' '' $false),
            (New-Edge 'student' 'baseCat' '' $false),
            (New-Edge 'admin' 'aiCat' '' $false),
            (New-Edge 'teacher' 'aiCat' '' $false),
            (New-Edge 'student' 'aiCat' '' $false),
            (New-Edge 'admin' 'adminCat' '' $false),
            (New-Edge 'teacher' 'teacherCat' '' $false),
            (New-Edge 'student' 'studentCat' '' $false),
            (New-Edge 'baseCat' 'login' '' $false $true),
            (New-Edge 'aiCat' 'chat' '' $false $true),
            (New-Edge 'aiCat' 'rag' '' $false $true),
            (New-Edge 'aiCat' 'searchAi' '' $false $true),
            (New-Edge 'aiCat' 'queryAi' '' $false $true),
            (New-Edge 'adminCat' 'admin1' '' $false $true),
            (New-Edge 'adminCat' 'admin2' '' $false $true),
            (New-Edge 'adminCat' 'admin3' '' $false $true),
            (New-Edge 'teacherCat' 'teacher1' '' $false $true),
            (New-Edge 'teacherCat' 'teacher2' '' $false $true),
            (New-Edge 'teacherCat' 'teacher3' '' $false $true),
            (New-Edge 'studentCat' 'student1' '' $false $true),
            (New-Edge 'studentCat' 'student2' '' $false $true),
            (New-Edge 'studentCat' 'student3' '' $false $true)
        )
    },
    @{
        Folder = $ThesisDir
        File = '03-智能问答主流程图'
        Title = '智能问答主流程图'
        Width = 1400
        Height = 1380
        Nodes = @(
            (New-Node 's' 'ellipse' '开始' 560 90 220 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold'),
            (New-Node 'p1' 'parallelogram' '用户输入问题与附件' 500 210 340 80 $Theme.OrangeFill $Theme.OrangeStroke 18),
            (New-Node 'p2' 'rect' '读取登录身份与会话记忆' 500 340 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'd1' 'diamond' '选择问答模式' 530 480 280 120 $Theme.YellowFill $Theme.YellowStroke 18),
            (New-Node 'direct' 'rect' 'DIRECT: 直接组装用户消息' 140 700 290 90 $Theme.BlueFill $Theme.BlueStroke 16),
            (New-Node 'rag' 'rect' 'KNOWLEDGE_BASE: 向量检索上下文' 555 700 290 90 $Theme.GreenFill $Theme.GreenStroke 16),
            (New-Node 'search' 'rect' 'INTERNET_SEARCH: 搜索并筛选结果' 970 700 290 90 $Theme.OrangeFill $Theme.OrangeStroke 16),
            (New-Node 'merge' 'rect' '统一拼接 Prompt 与模型参数' 500 930 340 80 $Theme.PurpleFill $Theme.PurpleStroke 18),
            (New-Node 'model' 'rect' '调用大模型生成回答' 500 1060 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'save' 'rect' '保存消息与更新会话' 500 1190 340 80 $Theme.GreyFill $Theme.GreyStroke 18),
            (New-Node 'e' 'ellipse' '返回答案' 560 1300 220 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold')
        )
        Edges = @(
            (New-Edge 's' 'p1'),
            (New-Edge 'p1' 'p2'),
            (New-Edge 'p2' 'd1'),
            (New-Edge 'd1' 'direct' '直接对话'),
            (New-Edge 'd1' 'rag' '知识库'),
            (New-Edge 'd1' 'search' '联网搜索'),
            (New-Edge 'direct' 'merge'),
            (New-Edge 'rag' 'merge'),
            (New-Edge 'search' 'merge'),
            (New-Edge 'merge' 'model'),
            (New-Edge 'model' 'save'),
            (New-Edge 'save' 'e')
        )
    },
    @{
        Folder = $ThesisDir
        File = '04-系统总体架构图'
        Title = '系统总体架构图'
        Width = 1700
        Height = 1180
        Nodes = @(
            (New-Node 'u1' 'round' '管理员' 180 90 180 70 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'u2' 'round' '教师' 480 90 180 70 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'u3' 'round' '学生' 780 90 180 70 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'u4' 'round' '浏览器前端 Vue 3 + Element Plus' 1080 90 420 70 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'sec' 'round' '认证拦截层`nJwtInterceptor / RoleInterceptor' 590 240 520 90 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'ctl' 'round' '控制层`nAuth / Chat / Rag / CRUD Controllers' 590 390 520 90 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'svc' 'round' '服务层`nAuthService / ChatService / RAG / Course / Grade / User' 520 540 660 90 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 'repo' 'round' '数据访问层`nMyBatis Mapper + JDBC ChatMemoryRepository' 520 690 660 90 $Theme.YellowFill $Theme.YellowStroke 18 'Bold'),
            (New-Node 'mysql' 'round' 'MySQL`n业务数据 / 会话元数据' 250 880 260 90 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'redis' 'round' 'Redis Vector Store`n知识向量检索' 560 880 260 90 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'oss' 'round' 'OSS`n文件与图片存储' 870 880 260 90 $Theme.GreyFill $Theme.GreyStroke 18 'Bold'),
            (New-Node 'llm' 'round' 'LLM 平台`n通义 / GLM / GPT / Kimi / DeepSeek' 1180 860 320 110 $Theme.RedFill $Theme.RedStroke 18 'Bold'),
            (New-Node 'search' 'round' 'SearXNG`n联网搜索服务' 1180 1020 320 90 $Theme.RedFill $Theme.RedStroke 18 'Bold')
        )
        Edges = @(
            (New-Edge 'u1' 'u4' 'HTTP'),
            (New-Edge 'u2' 'u4' 'HTTP'),
            (New-Edge 'u3' 'u4' 'HTTP'),
            (New-Edge 'u4' 'sec' 'HTTP / SSE'),
            (New-Edge 'sec' 'ctl'),
            (New-Edge 'ctl' 'svc'),
            (New-Edge 'svc' 'repo'),
            (New-Edge 'repo' 'mysql'),
            (New-Edge 'svc' 'redis'),
            (New-Edge 'svc' 'oss'),
            (New-Edge 'svc' 'llm'),
            (New-Edge 'svc' 'search')
        )
    },
    @{
        Folder = $ThesisDir
        File = '05-核心ER总图'
        Title = '核心 ER 总图'
        Width = 1780
        Height = 1280
        Nodes = @(
            (New-Node 'users' 'table' 'users' 690 80 300 190 $Theme.BlueFill $Theme.BlueStroke 12 'Regular' @('PK id','username','password','role','email','status','created_time','updated_time')),
            (New-Node 'teachers' 'table' 'teachers' 250 350 300 220 $Theme.GreenFill $Theme.GreenStroke 12 'Regular' @('PK id','FK user_id','name','gender','phone','department','title')),
            (New-Node 'students' 'table' 'students' 1110 350 300 220 $Theme.GreenFill $Theme.GreenStroke 12 'Regular' @('PK id','FK user_id','name','gender','grade','major','class_name')),
            (New-Node 'courses' 'table' 'courses' 250 710 300 210 $Theme.YellowFill $Theme.YellowStroke 12 'Regular' @('PK id','course_name','FK teacher_id','credit','begin_date','end_date','schedule')),
            (New-Node 'grades' 'table' 'grades' 690 710 300 190 $Theme.YellowFill $Theme.YellowStroke 12 'Regular' @('PK id','FK student_id','FK course_id','score','semester','created_at')),
            (New-Node 'conversations' 'table' 'conversations' 1120 700 300 210 $Theme.PurpleFill $Theme.PurpleStroke 12 'Regular' @('PK id','FK user_id','conversation_uid','title','type','created_time')),
            (New-Node 'messages' 'table' 'messages' 1460 710 260 190 $Theme.PurpleFill $Theme.PurpleStroke 12 'Regular' @('PK id','FK conversation_id','sender','content','image_url','sequence')),
            (New-Node 'rag_documents' 'table' 'rag_documents' 1120 1010 300 210 $Theme.OrangeFill $Theme.OrangeStroke 12 'Regular' @('PK id','file_name','oss_url','FK uploaded_by','FK owner_user_id','knowledge_scope')),
            (New-Node 'operation_logs' 'table' 'operation_logs' 250 1020 300 160 $Theme.GreyFill $Theme.GreyStroke 12 'Regular' @('PK id','operator','action','created_at'))
        )
        Edges = @(
            (New-Edge 'users' 'teachers' '1:1' $false),
            (New-Edge 'users' 'students' '1:1' $false),
            (New-Edge 'teachers' 'courses' '1:N' $false),
            (New-Edge 'students' 'grades' '1:N' $false),
            (New-Edge 'courses' 'grades' '1:N' $false),
            (New-Edge 'users' 'conversations' '1:N' $false),
            (New-Edge 'conversations' 'messages' '1:N' $false),
            (New-Edge 'users' 'rag_documents' '1:N' $false),
            (New-Edge 'users' 'operation_logs' '1:N' $false)
        )
    },
    @{
        Folder = $SupplementDir
        File = '06-管理员用例图'
        Title = '管理员用例图'
        Width = 1600
        Height = 1100
        Boundary = @{
            X = 240; Y = 100; W = 1300; H = 900; Label = '管理员模块'; Stroke = $Theme.OrangeStroke
        }
        Nodes = @(
            (New-Node 'admin' 'actor' '管理员' 80 455 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'sysCat' 'round' '系统管理' 430 200 220 84 $Theme.PurpleFill $Theme.PurpleStroke 18 'Bold'),
            (New-Node 'bizCat' 'round' '业务管理' 430 470 220 84 $Theme.OrangeFill $Theme.OrangeStroke 18 'Bold'),
            (New-Node 'aiCat' 'round' 'AI 功能' 430 760 220 84 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'a1' 'ellipse' '用户权限（增/改/查/删）' 860 160 330 72 '#ffffff' $Theme.PurpleStroke 16),
            (New-Node 'a2' 'ellipse' '注册密钥与日志' 860 260 260 72 '#ffffff' $Theme.PurpleStroke 16),
            (New-Node 'a3' 'ellipse' '档案管理（师/生，增/改/查/删）' 820 390 410 72 '#ffffff' $Theme.OrangeStroke 16),
            (New-Node 'a4' 'ellipse' '课程成绩（增/改/查/删）' 860 490 330 72 '#ffffff' $Theme.OrangeStroke 16),
            (New-Node 'a5' 'ellipse' '公共知识库管理' 860 590 260 72 '#ffffff' $Theme.OrangeStroke 16),
            (New-Node 'a6' 'ellipse' '智能问答（对话/知识库/联网）' 820 720 400 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 'a7' 'ellipse' 'AI 教务数据查询' 860 820 260 72 '#ffffff' $Theme.BlueStroke 16)
        )
        Edges = @(
            (New-Edge 'admin' 'sysCat' '' $false),
            (New-Edge 'admin' 'bizCat' '' $false),
            (New-Edge 'admin' 'aiCat' '' $false),
            (New-Edge 'sysCat' 'a1' '' $false $true),
            (New-Edge 'sysCat' 'a2' '' $false $true),
            (New-Edge 'bizCat' 'a3' '' $false $true),
            (New-Edge 'bizCat' 'a4' '' $false $true),
            (New-Edge 'bizCat' 'a5' '' $false $true),
            (New-Edge 'aiCat' 'a6' '' $false $true),
            (New-Edge 'aiCat' 'a7' '' $false $true)
        )
    },
    @{
        Folder = $SupplementDir
        File = '07-教师用例图'
        Title = '教师用例图'
        Width = 1560
        Height = 1020
        Boundary = @{
            X = 240; Y = 100; W = 1260; H = 820; Label = '教师模块'; Stroke = $Theme.OrangeStroke
        }
        Nodes = @(
            (New-Node 'teacher' 'actor' '教师' 80 405 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'aiCat' 'round' 'AI 模块' 430 245 220 84 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'teachCat' 'round' '教学管理' 430 625 220 84 $Theme.GreenFill $Theme.GreenStroke 18 'Bold'),
            (New-Node 't1' 'ellipse' '智能问答（对话/知识库/联网）' 820 160 400 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 't2' 'ellipse' 'AI 教务数据查询' 860 260 260 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 't3' 'ellipse' '私有知识库（上/改/删/查）' 840 360 320 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 't4' 'ellipse' '学生信息（查/改）' 860 540 260 72 '#ffffff' $Theme.GreenStroke 16),
            (New-Node 't5' 'ellipse' '课程维护（增/改/查）' 860 640 280 72 '#ffffff' $Theme.GreenStroke 16),
            (New-Node 't6' 'ellipse' '成绩维护（增/改/查）' 860 740 280 72 '#ffffff' $Theme.GreenStroke 16)
        )
        Edges = @(
            (New-Edge 'teacher' 'aiCat' '' $false),
            (New-Edge 'teacher' 'teachCat' '' $false),
            (New-Edge 'aiCat' 't1' '' $false $true),
            (New-Edge 'aiCat' 't2' '' $false $true),
            (New-Edge 'aiCat' 't3' '' $false $true),
            (New-Edge 'teachCat' 't4' '' $false $true),
            (New-Edge 'teachCat' 't5' '' $false $true),
            (New-Edge 'teachCat' 't6' '' $false $true)
        )
    },
    @{
        Folder = $SupplementDir
        File = '08-学生用例图'
        Title = '学生用例图'
        Width = 1560
        Height = 980
        Boundary = @{
            X = 240; Y = 100; W = 1260; H = 760; Label = '学生模块'; Stroke = $Theme.OrangeStroke
        }
        Nodes = @(
            (New-Node 'student' 'actor' '学生' 80 330 90 170 '#ffffff' $Theme.OrangeStroke 18),
            (New-Node 'aiCat' 'round' 'AI 模块' 430 220 220 84 $Theme.BlueFill $Theme.BlueStroke 18 'Bold'),
            (New-Node 'queryCat' 'round' '信息查询' 430 560 220 84 $Theme.YellowFill $Theme.YellowStroke 18 'Bold'),
            (New-Node 's1' 'ellipse' '智能问答（对话/知识库/联网）' 820 180 400 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 's2' 'ellipse' 'AI 教务数据查询' 860 280 260 72 '#ffffff' $Theme.BlueStroke 16),
            (New-Node 's3' 'ellipse' '课程信息（查）' 860 470 240 72 '#ffffff' $Theme.YellowStroke 16),
            (New-Node 's4' 'ellipse' '成绩信息（查）' 860 570 240 72 '#ffffff' $Theme.YellowStroke 16),
            (New-Node 's5' 'ellipse' '个人信息（查）' 860 670 240 72 '#ffffff' $Theme.YellowStroke 16)
        )
        Edges = @(
            (New-Edge 'student' 'aiCat' '' $false),
            (New-Edge 'student' 'queryCat' '' $false),
            (New-Edge 'aiCat' 's1' '' $false $true),
            (New-Edge 'aiCat' 's2' '' $false $true),
            (New-Edge 'queryCat' 's3' '' $false $true),
            (New-Edge 'queryCat' 's4' '' $false $true),
            (New-Edge 'queryCat' 's5' '' $false $true)
        )
    },
    @{
        Folder = $SupplementDir
        File = '09-RAG知识库处理流程图'
        Title = 'RAG 知识库处理流程图'
        Width = 1450
        Height = 1500
        Nodes = @(
            (New-Node 's' 'ellipse' '开始' 600 80 220 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold'),
            (New-Node 'p1' 'parallelogram' '上传知识文档' 540 190 340 80 $Theme.OrangeFill $Theme.OrangeStroke 18),
            (New-Node 'p2' 'rect' '写入 OSS 并记录 rag_documents' 540 320 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'd1' 'diamond' '文档是否包含图片或扫描页' 570 450 280 120 $Theme.YellowFill $Theme.YellowStroke 18),
            (New-Node 'ocr' 'rect' '调用 OCR / Tika 抽取文本' 220 670 320 90 $Theme.GreenFill $Theme.GreenStroke 17),
            (New-Node 'plain' 'rect' '直接读取文本内容' 900 670 260 90 $Theme.BlueFill $Theme.BlueStroke 17),
            (New-Node 'split' 'rect' 'TokenTextSplitter 文本切片' 540 860 340 80 $Theme.PurpleFill $Theme.PurpleStroke 18),
            (New-Node 'embed' 'rect' '批量向量化并写入 Redis' 540 990 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'query' 'parallelogram' '收到用户问题' 540 1120 340 80 $Theme.OrangeFill $Theme.OrangeStroke 18),
            (New-Node 'search' 'rect' '向量相似检索 Top-K 片段' 540 1250 340 80 $Theme.GreenFill $Theme.GreenStroke 18),
            (New-Node 'answer' 'ellipse' '拼接上下文并生成答案' 560 1370 300 80 $Theme.GreenFill $Theme.GreenStroke 18 'Bold')
        )
        Edges = @(
            (New-Edge 's' 'p1'),
            (New-Edge 'p1' 'p2'),
            (New-Edge 'p2' 'd1'),
            (New-Edge 'd1' 'ocr' '是'),
            (New-Edge 'd1' 'plain' '否'),
            (New-Edge 'ocr' 'split'),
            (New-Edge 'plain' 'split'),
            (New-Edge 'split' 'embed'),
            (New-Edge 'embed' 'query'),
            (New-Edge 'query' 'search'),
            (New-Edge 'search' 'answer')
        )
    },
    @{
        Folder = $SupplementDir
        File = '10-联网搜索流程图'
        Title = '联网搜索流程图'
        Width = 1400
        Height = 1320
        Nodes = @(
            (New-Node 's' 'ellipse' '开始' 580 90 220 70 $Theme.GreenFill $Theme.GreenStroke 20 'Bold'),
            (New-Node 'q' 'parallelogram' '用户输入问题' 520 210 340 80 $Theme.OrangeFill $Theme.OrangeStroke 18),
            (New-Node 'mode' 'rect' '选择 INTERNET_SEARCH 模式' 520 340 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'api' 'rect' '调用 SearXNG 搜索接口' 520 470 340 80 $Theme.GreenFill $Theme.GreenStroke 18),
            (New-Node 'rank' 'rect' '筛选并排序 Top-N 结果' 520 600 340 80 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'd1' 'diamond' '是否获得有效结果' 550 730 280 120 $Theme.YellowFill $Theme.YellowStroke 18),
            (New-Node 'fallback' 'rect' '返回未检索到可靠结果提示' 170 950 320 90 $Theme.RedFill $Theme.RedStroke 17),
            (New-Node 'prompt' 'rect' '拼接搜索摘要与用户问题' 780 950 320 90 $Theme.PurpleFill $Theme.PurpleStroke 17),
            (New-Node 'llm' 'rect' '调用大模型生成回答' 780 1090 320 90 $Theme.BlueFill $Theme.BlueStroke 18),
            (New-Node 'e' 'ellipse' '返回答案' 800 1220 280 80 $Theme.GreenFill $Theme.GreenStroke 18 'Bold')
        )
        Edges = @(
            (New-Edge 's' 'q'),
            (New-Edge 'q' 'mode'),
            (New-Edge 'mode' 'api'),
            (New-Edge 'api' 'rank'),
            (New-Edge 'rank' 'd1'),
            (New-Edge 'd1' 'fallback' '否'),
            (New-Edge 'd1' 'prompt' '是'),
            (New-Edge 'prompt' 'llm'),
            (New-Edge 'llm' 'e')
        )
    },
    @{
        Folder = $SupplementDir
        File = '11-用户权限与教务ER图'
        Title = '用户权限与教务 ER 图'
        Width = 1750
        Height = 1230
        Nodes = @(
            (New-Node 'users' 'table' 'users' 710 80 300 190 $Theme.BlueFill $Theme.BlueStroke 12 'Regular' @('PK id','username','password','role','email','status')),
            (New-Node 'teachers' 'table' 'teachers' 220 360 300 210 $Theme.GreenFill $Theme.GreenStroke 12 'Regular' @('PK id','FK user_id','name','department','title','phone')),
            (New-Node 'students' 'table' 'students' 1210 360 300 210 $Theme.GreenFill $Theme.GreenStroke 12 'Regular' @('PK id','FK user_id','name','grade','major','class_name')),
            (New-Node 'courses' 'table' 'courses' 220 740 300 210 $Theme.YellowFill $Theme.YellowStroke 12 'Regular' @('PK id','course_name','FK teacher_id','credit','schedule','description')),
            (New-Node 'grades' 'table' 'grades' 710 740 300 180 $Theme.YellowFill $Theme.YellowStroke 12 'Regular' @('PK id','FK student_id','FK course_id','score','semester')),
            (New-Node 'registration_keys' 'table' 'registration_keys' 1210 760 300 180 $Theme.OrangeFill $Theme.OrangeStroke 12 'Regular' @('PK id','key_value','used','used_by','used_at')),
            (New-Node 'operation_logs' 'table' 'operation_logs' 710 1020 300 160 $Theme.GreyFill $Theme.GreyStroke 12 'Regular' @('PK id','operator','action','created_at'))
        )
        Edges = @(
            (New-Edge 'users' 'teachers' '1:1' $false),
            (New-Edge 'users' 'students' '1:1' $false),
            (New-Edge 'teachers' 'courses' '1:N' $false),
            (New-Edge 'students' 'grades' '1:N' $false),
            (New-Edge 'courses' 'grades' '1:N' $false),
            (New-Edge 'users' 'registration_keys' '1:N' $false),
            (New-Edge 'users' 'operation_logs' '1:N' $false)
        )
    },
    @{
        Folder = $SupplementDir
        File = '12-AI会话与知识库ER图'
        Title = 'AI 会话与知识库 ER 图'
        Width = 1700
        Height = 1140
        Nodes = @(
            (New-Node 'users' 'table' 'users' 150 380 300 180 $Theme.BlueFill $Theme.BlueStroke 12 'Regular' @('PK id','username','role','email','status')),
            (New-Node 'conversations' 'table' 'conversations' 590 130 300 220 $Theme.PurpleFill $Theme.PurpleStroke 12 'Regular' @('PK id','FK user_id','conversation_uid','title','type','created_time')),
            (New-Node 'messages' 'table' 'messages' 1070 130 300 210 $Theme.PurpleFill $Theme.PurpleStroke 12 'Regular' @('PK id','FK conversation_id','sender','content','image_url','sequence')),
            (New-Node 'rag_documents' 'table' 'rag_documents' 590 640 320 220 $Theme.OrangeFill $Theme.OrangeStroke 12 'Regular' @('PK id','file_name','oss_url','FK uploaded_by','FK owner_user_id','knowledge_scope')),
            (New-Node 'rag_ocr_user_settings' 'table' 'rag_ocr_user_settings' 1070 640 320 200 $Theme.GreenFill $Theme.GreenStroke 12 'Regular' @('PK id','FK user_id','base_url','api_key','model','updated_at'))
        )
        Edges = @(
            (New-Edge 'users' 'conversations' '1:N' $false),
            (New-Edge 'conversations' 'messages' '1:N' $false),
            (New-Edge 'users' 'rag_documents' '1:N' $false),
            (New-Edge 'users' 'rag_ocr_user_settings' '1:1' $false)
        )
    }
)

foreach ($diagram in $Diagrams) {
    $basePath = Join-Path $diagram.Folder $diagram.File
    Save-Drawio $diagram $basePath
    Save-Png $diagram $basePath
}

$readme = @"
# 系统介绍图集

本目录按“论文主图 + 技术补充图”组织系统介绍图，所有图均同时提供 `.drawio` 源文件与 `.png` 预览图。

## 论文主图

1. `论文主图/01-系统总体功能结构图`
2. `论文主图/02-系统总用例图`
3. `论文主图/03-智能问答主流程图`
4. `论文主图/04-系统总体架构图`
5. `论文主图/05-核心ER总图`

## 技术补充图

6. `技术补充图/06-管理员用例图`
7. `技术补充图/07-教师用例图`
8. `技术补充图/08-学生用例图`
9. `技术补充图/09-RAG知识库处理流程图`
10. `技术补充图/10-联网搜索流程图`
11. `技术补充图/11-用户权限与教务ER图`
12. `技术补充图/12-AI会话与知识库ER图`

## 说明

- 论文主图用于系统概述、总体设计、数据库设计与 AI 功能设计章节。
- 技术补充图用于答辩展示或项目文档补充说明。
- 若需继续微调版式，优先编辑对应的 `.drawio` 文件。
"@

[System.IO.File]::WriteAllText((Join-Path $OutputRoot '图集说明.md'), $readme, [System.Text.Encoding]::UTF8)

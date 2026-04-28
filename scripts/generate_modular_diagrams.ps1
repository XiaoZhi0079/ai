param(
    [string]$OutputDir = 'C:\Users\张海松\Desktop\ai\毕设文档\picture'
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

function Ensure-Dir([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Xml-Escape([string]$Text) {
    if ($null -eq $Text) { return '' }
    return $Text.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;').Replace('"', '&quot;').Replace("`n", '&#xa;')
}

function RectF([double]$x, [double]$y, [double]$w, [double]$h) {
    return [System.Drawing.RectangleF]::new([single]$x, [single]$y, [single]$w, [single]$h)
}

function New-Canvas([int]$Width, [int]$Height) {
    $bmp = New-Object System.Drawing.Bitmap $Width, $Height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.Color]::White)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Color([string]$Hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($Hex)
}

function PenEx([string]$Hex, [float]$Width = 2, [bool]$Arrow = $false) {
    $pen = New-Object System.Drawing.Pen (Color $Hex), $Width
    if ($Arrow) {
        $pen.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(6, 8, $false)
    }
    return $pen
}

function BrushEx([string]$Hex) {
    return New-Object System.Drawing.SolidBrush (Color $Hex)
}

function FontEx([float]$Size, [string]$Style = 'Regular') {
    return New-Object System.Drawing.Font('Microsoft YaHei', $Size, [System.Drawing.FontStyle]::$Style, [System.Drawing.GraphicsUnit]::Pixel)
}

function Draw-Grid($g, [int]$Width, [int]$Height) {
    $pen = New-Object System.Drawing.Pen (Color '#eee7dc'), 1
    for ($x = 0; $x -le $Width; $x += 40) { $g.DrawLine($pen, $x, 0, $x, $Height) }
    for ($y = 0; $y -le $Height; $y += 40) { $g.DrawLine($pen, 0, $y, $Width, $y) }
    $pen.Dispose()
}

function Center-Text($g, [string]$Text, $Rect, $Font, $Brush) {
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($Text, $Font, $Brush, $Rect, $fmt)
    $fmt.Dispose()
}

function Draw-RoundedBorder($g, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = 12
    $d = $r * 2
    $path.AddArc($X, $Y, $d, $d, 180, 90)
    $path.AddArc($X + $W - $d, $Y, $d, $d, 270, 90)
    $path.AddArc($X + $W - $d, $Y + $H - $d, $d, $d, 0, 90)
    $path.AddArc($X, $Y + $H - $d, $d, $d, 90, 90)
    $path.CloseFigure()

    $fill = BrushEx '#ffffff'
    $pen = PenEx '#ff8a1d' 3
    $g.FillPath($fill, $path)
    $g.DrawPath($pen, $path)
    $fill.Dispose()
    $pen.Dispose()
    $path.Dispose()
}

function Draw-Actor($g, [int]$X, [int]$Y, [string]$Label) {
    $pen = PenEx '#ff8a1d' 3
    $font = FontEx 18
    $brush = BrushEx '#444444'
    $g.DrawEllipse($pen, $X + 18, $Y, 24, 24)
    $g.DrawLine($pen, $X + 30, $Y + 24, $X + 30, $Y + 66)
    $g.DrawLine($pen, $X, $Y + 40, $X + 60, $Y + 40)
    $g.DrawLine($pen, $X + 30, $Y + 66, $X, $Y + 96)
    $g.DrawLine($pen, $X + 30, $Y + 66, $X + 60, $Y + 96)
    Center-Text $g $Label (RectF ($X - 16) ($Y + 104) 92 30) $font $brush
    $pen.Dispose(); $font.Dispose(); $brush.Dispose()
}

function Draw-UseCase($g, [int]$X, [int]$Y, [int]$W, [int]$H, [string]$Label) {
    $pen = PenEx '#ff8a1d' 3
    $fill = BrushEx '#ffffff'
    $font = FontEx 18
    $brush = BrushEx '#444444'
    $g.FillEllipse($fill, $X, $Y, $W, $H)
    $g.DrawEllipse($pen, $X, $Y, $W, $H)
    Center-Text $g $Label (RectF $X $Y $W $H) $font $brush
    $pen.Dispose(); $fill.Dispose(); $font.Dispose(); $brush.Dispose()
}

function Draw-Entity($g, [int]$X, [int]$Y, [int]$W, [int]$H, [string]$Label) {
    $pen = PenEx '#ff8a1d' 3
    $fill = BrushEx '#ff8a1d'
    $font = FontEx 18 'Bold'
    $brush = BrushEx '#111111'
    $g.FillRectangle($fill, $X, $Y, $W, $H)
    $g.DrawRectangle($pen, $X, $Y, $W, $H)
    Center-Text $g $Label (RectF $X $Y $W $H) $font $brush
    $pen.Dispose(); $fill.Dispose(); $font.Dispose(); $brush.Dispose()
}

function Draw-Attribute($g, [int]$X, [int]$Y, [int]$W, [int]$H, [string]$Label) {
    Draw-UseCase $g $X $Y $W $H $Label
}

function Draw-Relation($g, [int]$X, [int]$Y, [int]$W, [int]$H, [string]$Label) {
    $pen = PenEx '#ff8a1d' 3
    $fill = BrushEx '#ffffff'
    $font = FontEx 15 'Bold'
    $brush = BrushEx '#444444'
    $pts = @(
        [System.Drawing.PointF]::new($X + $W / 2, $Y),
        [System.Drawing.PointF]::new($X + $W, $Y + $H / 2),
        [System.Drawing.PointF]::new($X + $W / 2, $Y + $H),
        [System.Drawing.PointF]::new($X, $Y + $H / 2)
    )
    $g.FillPolygon($fill, $pts)
    $g.DrawPolygon($pen, $pts)
    Center-Text $g $Label (RectF $X $Y $W $H) $font $brush
    $pen.Dispose(); $fill.Dispose(); $font.Dispose(); $brush.Dispose()
}

function Draw-Line($g, [int]$X1, [int]$Y1, [int]$X2, [int]$Y2, [bool]$Arrow = $false) {
    $pen = PenEx '#8a8a8a' 3 $Arrow
    $g.DrawLine($pen, $X1, $Y1, $X2, $Y2)
    $pen.Dispose()
}

function Edge-Point($Shape, [double]$TargetX, [double]$TargetY) {
    $cx = $Shape.X + ($Shape.W / 2.0)
    $cy = $Shape.Y + ($Shape.H / 2.0)
    $dx = $TargetX - $cx
    $dy = $TargetY - $cy
    if ([math]::Abs($dx) -ge [math]::Abs($dy)) {
        if ($dx -ge 0) { return @{ X = [int]($Shape.X + $Shape.W); Y = [int]$cy } }
        return @{ X = [int]$Shape.X; Y = [int]$cy }
    }
    if ($dy -ge 0) { return @{ X = [int]$cx; Y = [int]($Shape.Y + $Shape.H) } }
    return @{ X = [int]$cx; Y = [int]$Shape.Y }
}

function Save-Drawio([string]$Path, [int]$Width, [int]$Height, [array]$Cells) {
    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine('<mxfile host="drawio" version="26.0.0">')
    [void]$sb.AppendLine('  <diagram name="Page-1">')
    [void]$sb.AppendLine("    <mxGraphModel page=`"1`" pageWidth=`"$Width`" pageHeight=`"$Height`" grid=`"1`" gridSize=`"10`">")
    [void]$sb.AppendLine('      <root>')
    [void]$sb.AppendLine('        <mxCell id="0" />')
    [void]$sb.AppendLine('        <mxCell id="1" parent="0" />')
    foreach ($cell in $Cells) {
        if ($cell.Type -eq 'edge') {
            $value = Xml-Escape $cell.Value
            [void]$sb.AppendLine("        <mxCell id=`"$($cell.Id)`" value=`"$value`" style=`"$($cell.Style)`" edge=`"1`" parent=`"1`" source=`"$($cell.Source)`" target=`"$($cell.Target)`">")
            [void]$sb.AppendLine('          <mxGeometry relative="1" as="geometry" />')
            [void]$sb.AppendLine('        </mxCell>')
        } else {
            $value = Xml-Escape $cell.Value
            [void]$sb.AppendLine("        <mxCell id=`"$($cell.Id)`" value=`"$value`" style=`"$($cell.Style)`" vertex=`"1`" parent=`"1`">")
            [void]$sb.AppendLine("          <mxGeometry x=`"$($cell.X)`" y=`"$($cell.Y)`" width=`"$($cell.W)`" height=`"$($cell.H)`" as=`"geometry`" />")
            [void]$sb.AppendLine('        </mxCell>')
        }
    }
    [void]$sb.AppendLine('      </root>')
    [void]$sb.AppendLine('    </mxGraphModel>')
    [void]$sb.AppendLine('  </diagram>')
    [void]$sb.AppendLine('</mxfile>')
    [System.IO.File]::WriteAllText($Path, $sb.ToString(), [System.Text.Encoding]::UTF8)
}

function UseCase-Cells($Title, $Actor, $UseCases) {
    $cells = @()
    $cells += [pscustomobject]@{ Id = '2'; Type = 'vertex'; Value = ''; Style = 'rounded=1;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#ff8a1d;strokeWidth=2;'; X = 180; Y = 30; W = 950; H = 780 }
    $cells += [pscustomobject]@{ Id = '3'; Type = 'vertex'; Value = $Title; Style = 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=22;fontStyle=1;fontColor=#333333;'; X = 520; Y = 45; W = 260; H = 30 }
    $cells += [pscustomobject]@{ Id = '10'; Type = 'vertex'; Value = $Actor; Style = 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;strokeColor=#ff8a1d;fontColor=#333333;'; X = 40; Y = 310; W = 70; H = 120 }
    $i = 0
    foreach ($uc in $UseCases) {
        $col = [math]::Floor($i / 4)
        $row = $i % 4
        $x = 300 + ($col * 330)
        $y = 120 + ($row * 150)
        $w = if ($uc.Length -ge 8) { 240 } else { 200 }
        $id = 20 + $i
        $cells += [pscustomobject]@{
            Id = [string]$id
            Type = 'vertex'
            Value = $uc
            Style = 'ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#ff8a1d;strokeWidth=2;fontSize=18;'
            X = $x; Y = $y; W = $w; H = 70
        }
        $cells += [pscustomobject]@{
            Id = "e$id"
            Type = 'edge'
            Value = ''
            Style = 'edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#8a8a8a;strokeWidth=2;endArrow=block;endFill=1;'
            Source = '10'; Target = [string]$id
        }
        $i++
    }
    return $cells
}

function Render-UseCaseDiagram($Spec, [string]$BasePath) {
    $cells = UseCase-Cells $Spec.Title $Spec.Actor $Spec.UseCases
    Save-Drawio "$BasePath.drawio" 1200 860 $cells

    $canvas = New-Canvas 1200 860
    $g = $canvas.Graphics
    Draw-Grid $g 1200 860
    Draw-RoundedBorder $g 180 30 950 780

    $titleFont = FontEx 20 'Bold'
    $titleBrush = BrushEx '#444444'
    Center-Text $g $Spec.Title (RectF 480 45 360 30) $titleFont $titleBrush
    $titleFont.Dispose(); $titleBrush.Dispose()

    Draw-Actor $g 40 310 $Spec.Actor
    $i = 0
    foreach ($uc in $Spec.UseCases) {
        $col = [math]::Floor($i / 4)
        $row = $i % 4
        $x = 300 + ($col * 330)
        $y = 120 + ($row * 150)
        $w = if ($uc.Length -ge 8) { 240 } else { 200 }
        Draw-UseCase $g $x $y $w 70 $uc
        Draw-Line $g 110 370 $x ($y + 35) $true
        $i++
    }

    $canvas.Bitmap.Save("$BasePath.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $canvas.Bitmap.Dispose()
}

function Render-ErDiagram($Spec, [string]$BasePath) {
    $cells = @()
    $cells += [pscustomobject]@{ Id = '2'; Type = 'vertex'; Value = $Spec.Title; Style = 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=22;fontStyle=1;fontColor=#333333;'; X = 420; Y = 25; W = 420; H = 30 }
    foreach ($shape in $Spec.Shapes) {
        $style = switch ($shape.Kind) {
            'entity' { 'rounded=0;whiteSpace=wrap;html=1;fillColor=#ff8a1d;strokeColor=#ff8a1d;fontSize=18;fontStyle=1;fontColor=#111111;' }
            'attribute' { 'ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#ff8a1d;strokeWidth=2;fontSize=15;' }
            'relation' { 'rhombus;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#ff8a1d;strokeWidth=2;fontSize=14;fontStyle=1;' }
        }
        $cells += [pscustomobject]@{
            Id = $shape.Id; Type = 'vertex'; Value = $shape.Label; Style = $style
            X = $shape.X; Y = $shape.Y; W = $shape.W; H = $shape.H
        }
    }
    foreach ($line in $Spec.Lines) {
        $cells += [pscustomobject]@{
            Id = "e$($line.Id)"; Type = 'edge'; Value = ''
            Style = 'edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#8a8a8a;strokeWidth=2;endArrow=none;'
            Source = $line.Source; Target = $line.Target
        }
    }
    Save-Drawio "$BasePath.drawio" 1200 900 $cells

    $canvas = New-Canvas 1200 900
    $g = $canvas.Graphics
    Draw-Grid $g 1200 900
    $font = FontEx 22 'Bold'
    $brush = BrushEx '#444444'
    Center-Text $g $Spec.Title (RectF 390 25 420 30) $font $brush
    $font.Dispose(); $brush.Dispose()

    foreach ($shape in $Spec.Shapes) {
        switch ($shape.Kind) {
            'entity' { Draw-Entity $g $shape.X $shape.Y $shape.W $shape.H $shape.Label }
            'attribute' { Draw-Attribute $g $shape.X $shape.Y $shape.W $shape.H $shape.Label }
            'relation' { Draw-Relation $g $shape.X $shape.Y $shape.W $shape.H $shape.Label }
        }
    }
    foreach ($line in $Spec.Lines) {
        $s = $Spec.Shapes | Where-Object Id -eq $line.Source | Select-Object -First 1
        $t = $Spec.Shapes | Where-Object Id -eq $line.Target | Select-Object -First 1
        if ($s -and $t) {
            $targetCenterX = $t.X + ($t.W / 2.0)
            $targetCenterY = $t.Y + ($t.H / 2.0)
            $sourceCenterX = $s.X + ($s.W / 2.0)
            $sourceCenterY = $s.Y + ($s.H / 2.0)
            $p1 = Edge-Point $s $targetCenterX $targetCenterY
            $p2 = Edge-Point $t $sourceCenterX $sourceCenterY
            Draw-Line $g $p1.X $p1.Y $p2.X $p2.Y $false
        }
    }
    $canvas.Bitmap.Save("$BasePath.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $canvas.Bitmap.Dispose()
}

Ensure-Dir $OutputDir

$useCaseSpecs = @(
    @{
        File = '用例图-管理员模块'
        Title = '管理员模块用例图'
        Actor = '管理员'
        UseCases = @('用户管理', '教师管理', '学生管理', '课程管理', '成绩管理', '生成注册密钥', '公共知识库管理')
    },
    @{
        File = '用例图-教师模块'
        Title = '教师模块用例图'
        Actor = '教师'
        UseCases = @('智能对话', 'AI 数据查询', '个人知识库管理', '课程管理', '成绩管理', '学生信息维护')
    },
    @{
        File = '用例图-学生模块'
        Title = '学生模块用例图'
        Actor = '学生'
        UseCases = @('智能对话', 'AI 数据查询', '课程查看', '成绩查看', '教务信息查询', '对话历史管理')
    }
)

$erSpecs = @(
    @{
        File = 'ER图-用户与权限模块'
        Title = '用户与权限模块 ER 图'
        Shapes = @(
            @{ Id='u'; Kind='entity'; Label='用户'; X=490; Y=140; W=180; H=60 },
            @{ Id='rk'; Kind='entity'; Label='注册密钥'; X=140; Y=420; W=180; H=60 },
            @{ Id='ol'; Kind='entity'; Label='操作日志'; X=840; Y=420; W=180; H=60 },
            @{ Id='a1'; Kind='attribute'; Label='用户名'; X=420; Y=60; W=120; H=50 },
            @{ Id='a2'; Kind='attribute'; Label='角色'; X=620; Y=60; W=100; H=50 },
            @{ Id='a3'; Kind='attribute'; Label='邮箱'; X=690; Y=150; W=110; H=50 },
            @{ Id='a4'; Kind='attribute'; Label='状态'; X=370; Y=150; W=100; H=50 },
            @{ Id='a5'; Kind='attribute'; Label='密钥值'; X=70; Y=360; W=120; H=50 },
            @{ Id='a6'; Kind='attribute'; Label='是否使用'; X=270; Y=360; W=120; H=50 },
            @{ Id='a7'; Kind='attribute'; Label='操作者'; X=810; Y=360; W=120; H=50 },
            @{ Id='a8'; Kind='attribute'; Label='操作内容'; X=980; Y=360; W=120; H=50 },
            @{ Id='r1'; Kind='relation'; Label='0..1:N'; X=330; Y=300; W=100; H=80 },
            @{ Id='r2'; Kind='relation'; Label='1:N'; X=720; Y=300; W=80; H=70 }
        )
        Lines = @(
            @{ Id='1'; Source='u'; Target='a1' }, @{ Id='2'; Source='u'; Target='a2' }, @{ Id='3'; Source='u'; Target='a3' }, @{ Id='4'; Source='u'; Target='a4' },
            @{ Id='5'; Source='rk'; Target='a5' }, @{ Id='6'; Source='rk'; Target='a6' },
            @{ Id='7'; Source='ol'; Target='a7' }, @{ Id='8'; Source='ol'; Target='a8' },
            @{ Id='9'; Source='u'; Target='r1' }, @{ Id='10'; Source='r1'; Target='rk' },
            @{ Id='11'; Source='u'; Target='r2' }, @{ Id='12'; Source='r2'; Target='ol' }
        )
    },
    @{
        File = 'ER图-学生管理模块'
        Title = '学生管理模块 ER 图'
        Shapes = @(
            @{ Id='u'; Kind='entity'; Label='用户'; X=210; Y=220; W=180; H=60 },
            @{ Id='s'; Kind='entity'; Label='学生'; X=760; Y=220; W=180; H=60 },
            @{ Id='a1'; Kind='attribute'; Label='用户名'; X=150; Y=120; W=120; H=50 },
            @{ Id='a2'; Kind='attribute'; Label='角色'; X=320; Y=120; W=100; H=50 },
            @{ Id='a3'; Kind='attribute'; Label='姓名'; X=730; Y=120; W=100; H=50 },
            @{ Id='a4'; Kind='attribute'; Label='年级'; X=920; Y=140; W=100; H=50 },
            @{ Id='a5'; Kind='attribute'; Label='专业'; X=920; Y=240; W=100; H=50 },
            @{ Id='a6'; Kind='attribute'; Label='班级'; X=730; Y=320; W=100; H=50 },
            @{ Id='a7'; Kind='attribute'; Label='监护电话'; X=900; Y=330; W=140; H=50 },
            @{ Id='r1'; Kind='relation'; Label='1:1'; X=500; Y=210; W=80; H=70 }
        )
        Lines = @(
            @{ Id='1'; Source='u'; Target='a1' }, @{ Id='2'; Source='u'; Target='a2' },
            @{ Id='3'; Source='s'; Target='a3' }, @{ Id='4'; Source='s'; Target='a4' }, @{ Id='5'; Source='s'; Target='a5' }, @{ Id='6'; Source='s'; Target='a6' }, @{ Id='7'; Source='s'; Target='a7' },
            @{ Id='8'; Source='u'; Target='r1' }, @{ Id='9'; Source='r1'; Target='s' }
        )
    },
    @{
        File = 'ER图-教师与课程模块'
        Title = '教师与课程模块 ER 图'
        Shapes = @(
            @{ Id='u'; Kind='entity'; Label='用户'; X=110; Y=180; W=180; H=60 },
            @{ Id='t'; Kind='entity'; Label='教师'; X=470; Y=180; W=180; H=60 },
            @{ Id='c'; Kind='entity'; Label='课程'; X=840; Y=420; W=180; H=60 },
            @{ Id='a1'; Kind='attribute'; Label='用户名'; X=70; Y=90; W=120; H=50 },
            @{ Id='a2'; Kind='attribute'; Label='姓名'; X=470; Y=90; W=100; H=50 },
            @{ Id='a3'; Kind='attribute'; Label='院系'; X=650; Y=120; W=100; H=50 },
            @{ Id='a4'; Kind='attribute'; Label='职称'; X=650; Y=220; W=100; H=50 },
            @{ Id='a5'; Kind='attribute'; Label='课程名'; X=820; Y=330; W=120; H=50 },
            @{ Id='a6'; Kind='attribute'; Label='学分'; X=1000; Y=340; W=100; H=50 },
            @{ Id='a7'; Kind='attribute'; Label='课表'; X=1030; Y=450; W=100; H=50 },
            @{ Id='r1'; Kind='relation'; Label='1:1'; X=330; Y=170; W=80; H=70 },
            @{ Id='r2'; Kind='relation'; Label='1:N'; X=700; Y=330; W=80; H=70 }
        )
        Lines = @(
            @{ Id='1'; Source='u'; Target='a1' }, @{ Id='2'; Source='t'; Target='a2' }, @{ Id='3'; Source='t'; Target='a3' }, @{ Id='4'; Source='t'; Target='a4' },
            @{ Id='5'; Source='c'; Target='a5' }, @{ Id='6'; Source='c'; Target='a6' }, @{ Id='7'; Source='c'; Target='a7' },
            @{ Id='8'; Source='u'; Target='r1' }, @{ Id='9'; Source='r1'; Target='t' },
            @{ Id='10'; Source='t'; Target='r2' }, @{ Id='11'; Source='r2'; Target='c' }
        )
    },
    @{
        File = 'ER图-成绩管理模块'
        Title = '成绩管理模块 ER 图'
        Shapes = @(
            @{ Id='s'; Kind='entity'; Label='学生'; X=180; Y=310; W=180; H=60 },
            @{ Id='c'; Kind='entity'; Label='课程'; X=860; Y=310; W=180; H=60 },
            @{ Id='g'; Kind='entity'; Label='成绩'; X=520; Y=560; W=180; H=60 },
            @{ Id='a1'; Kind='attribute'; Label='姓名'; X=150; Y=220; W=100; H=50 },
            @{ Id='a2'; Kind='attribute'; Label='专业'; X=150; Y=390; W=100; H=50 },
            @{ Id='a3'; Kind='attribute'; Label='课程名'; X=870; Y=220; W=120; H=50 },
            @{ Id='a4'; Kind='attribute'; Label='学分'; X=1040; Y=390; W=100; H=50 },
            @{ Id='a5'; Kind='attribute'; Label='分数'; X=470; Y=660; W=100; H=50 },
            @{ Id='a6'; Kind='attribute'; Label='学期'; X=660; Y=660; W=100; H=50 },
            @{ Id='r1'; Kind='relation'; Label='1:N'; X=370; Y=470; W=80; H=70 },
            @{ Id='r2'; Kind='relation'; Label='1:N'; X=760; Y=470; W=80; H=70 }
        )
        Lines = @(
            @{ Id='1'; Source='s'; Target='a1' }, @{ Id='2'; Source='s'; Target='a2' },
            @{ Id='3'; Source='c'; Target='a3' }, @{ Id='4'; Source='c'; Target='a4' },
            @{ Id='5'; Source='g'; Target='a5' }, @{ Id='6'; Source='g'; Target='a6' },
            @{ Id='7'; Source='s'; Target='r1' }, @{ Id='8'; Source='r1'; Target='g' },
            @{ Id='9'; Source='c'; Target='r2' }, @{ Id='10'; Source='r2'; Target='g' }
        )
    },
    @{
        File = 'ER图-AI服务模块'
        Title = 'AI服务模块 ER 图'
        Shapes = @(
            @{ Id='u'; Kind='entity'; Label='用户'; X=120; Y=180; W=180; H=60 },
            @{ Id='cv'; Kind='entity'; Label='会话'; X=520; Y=120; W=180; H=60 },
            @{ Id='m'; Kind='entity'; Label='消息'; X=910; Y=120; W=180; H=60 },
            @{ Id='rd'; Kind='entity'; Label='知识文档'; X=460; Y=450; W=200; H=60 },
            @{ Id='ocr'; Kind='entity'; Label='OCR设置'; X=900; Y=450; W=180; H=60 },
            @{ Id='a1'; Kind='attribute'; Label='会话编号'; X=500; Y=40; W=140; H=50 },
            @{ Id='a2'; Kind='attribute'; Label='类型'; X=690; Y=60; W=100; H=50 },
            @{ Id='a3'; Kind='attribute'; Label='内容'; X=930; Y=40; W=100; H=50 },
            @{ Id='a4'; Kind='attribute'; Label='顺序'; X=1080; Y=60; W=100; H=50 },
            @{ Id='a5'; Kind='attribute'; Label='文件名'; X=410; Y=360; W=120; H=50 },
            @{ Id='a6'; Kind='attribute'; Label='范围'; X=650; Y=380; W=100; H=50 },
            @{ Id='a7'; Kind='attribute'; Label='BaseUrl'; X=860; Y=360; W=120; H=50 },
            @{ Id='a8'; Kind='attribute'; Label='模型'; X=1080; Y=380; W=100; H=50 },
            @{ Id='r1'; Kind='relation'; Label='1:N'; X=340; Y=150; W=80; H=70 },
            @{ Id='r2'; Kind='relation'; Label='1:N'; X=760; Y=150; W=80; H=70 },
            @{ Id='r3'; Kind='relation'; Label='1:N'; X=270; Y=420; W=80; H=70 },
            @{ Id='r4'; Kind='relation'; Label='1:1'; X=760; Y=420; W=80; H=70 }
        )
        Lines = @(
            @{ Id='1'; Source='cv'; Target='a1' }, @{ Id='2'; Source='cv'; Target='a2' },
            @{ Id='3'; Source='m'; Target='a3' }, @{ Id='4'; Source='m'; Target='a4' },
            @{ Id='5'; Source='rd'; Target='a5' }, @{ Id='6'; Source='rd'; Target='a6' },
            @{ Id='7'; Source='ocr'; Target='a7' }, @{ Id='8'; Source='ocr'; Target='a8' },
            @{ Id='9'; Source='u'; Target='r1' }, @{ Id='10'; Source='r1'; Target='cv' },
            @{ Id='11'; Source='cv'; Target='r2' }, @{ Id='12'; Source='r2'; Target='m' },
            @{ Id='13'; Source='u'; Target='r3' }, @{ Id='14'; Source='r3'; Target='rd' },
            @{ Id='15'; Source='u'; Target='r4' }, @{ Id='16'; Source='r4'; Target='ocr' }
        )
    }
)

foreach ($spec in $useCaseSpecs) {
    Render-UseCaseDiagram $spec (Join-Path $OutputDir $spec.File)
}

foreach ($spec in $erSpecs) {
    Render-ErDiagram $spec (Join-Path $OutputDir $spec.File)
}

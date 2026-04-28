from __future__ import annotations

import html
import re
import zipfile
from pathlib import Path


W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
ROOT = Path(__file__).resolve().parents[1]


def xml_escape(text: str) -> str:
    return html.escape(text, quote=False)


def make_run(
    text: str,
    *,
    ascii_font: str = "Times New Roman",
    east_asia_font: str = "SimSun",
    size: int = 24,
    bold: bool = False,
    color: str | None = None,
) -> str:
    preserve = ' xml:space="preserve"' if text[:1] == " " or text[-1:] == " " else ""
    bold_xml = "<w:b/><w:bCs/>" if bold else ""
    color_xml = f'<w:color w:val="{color}"/>' if color else ""
    return (
        "<w:r>"
        "<w:rPr>"
        f'<w:rFonts w:ascii="{ascii_font}" w:hAnsi="{ascii_font}" '
        f'w:eastAsia="{east_asia_font}" w:cs="{ascii_font}"/>'
        f"{bold_xml}"
        f"{color_xml}"
        f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>'
        "</w:rPr>"
        f"<w:t{preserve}>{xml_escape(text)}</w:t>"
        "</w:r>"
    )


def make_paragraph(
    text: str,
    *,
    bold: bool = False,
    first_line_indent: bool = True,
    left_indent: int | None = None,
    spacing_before: int | None = None,
    spacing_after: int | None = None,
    shading: str | None = None,
    border: bool = False,
    ascii_font: str = "Times New Roman",
    east_asia_font: str = "SimSun",
    size: int = 24,
    color: str | None = None,
) -> str:
    ppr_parts: list[str] = []
    if spacing_before is not None or spacing_after is not None:
        before = f' w:before="{spacing_before}"' if spacing_before is not None else ""
        after = f' w:after="{spacing_after}"' if spacing_after is not None else ""
        ppr_parts.append(f"<w:spacing{before}{after}/>")

    ind_parts: list[str] = []
    if first_line_indent:
        ind_parts.append('w:firstLineChars="200"')
    if left_indent is not None:
        ind_parts.append(f'w:left="{left_indent}"')
    if ind_parts:
        ppr_parts.append(f"<w:ind {' '.join(ind_parts)}/>")

    if shading:
        ppr_parts.append(f'<w:shd w:val="clear" w:color="auto" w:fill="{shading}"/>')
    if border:
        ppr_parts.append(
            "<w:pBdr>"
            '<w:top w:val="single" w:sz="4" w:space="2" w:color="D0D7DE"/>'
            '<w:left w:val="single" w:sz="4" w:space="2" w:color="D0D7DE"/>'
            '<w:bottom w:val="single" w:sz="4" w:space="2" w:color="D0D7DE"/>'
            '<w:right w:val="single" w:sz="4" w:space="2" w:color="D0D7DE"/>'
            "</w:pBdr>"
        )
    ppr_xml = f"<w:pPr>{''.join(ppr_parts)}</w:pPr>" if ppr_parts else ""
    return (
        f"<w:p>{ppr_xml}"
        f"{make_run(text, bold=bold, ascii_font=ascii_font, east_asia_font=east_asia_font, size=size, color=color)}"
        "</w:p>"
    )


def build_code_block_xml(code: str) -> str:
    lines = code.rstrip().splitlines() or [""]
    return "".join(
        make_paragraph(
            line,
            first_line_indent=False,
            left_indent=420,
            spacing_before=0,
            spacing_after=0,
            shading="F6F8FA",
            border=True,
            ascii_font="Courier New",
            east_asia_font="DengXian",
            size=20,
        )
        for line in lines
    ) + make_paragraph("", first_line_indent=False, spacing_after=60)


def insert_section_before_heading(document_xml: str, heading_text: str, section_xml: str) -> str:
    paragraph_pattern = re.compile(r"<w:p\b[\s\S]*?</w:p>")
    normalized_heading = normalize_visible_text(heading_text)
    for match in paragraph_pattern.finditer(document_xml):
        paragraph_xml = match.group(0)
        paragraph_text = normalize_visible_text(strip_xml_text(paragraph_xml))
        if normalized_heading in paragraph_text:
            return document_xml[:match.start()] + section_xml + document_xml[match.start():]
    raise ValueError(f"Heading not found: {heading_text}")


def insert_section_into_module(document_xml: str, module_heading: str, next_heading: str, section_xml: str) -> str:
    paragraph_pattern = re.compile(r"<w:p\b[\s\S]*?</w:p>")
    paragraphs: list[tuple[int, int, str]] = []
    for match in paragraph_pattern.finditer(document_xml):
        paragraph_xml = match.group(0)
        paragraph_text = normalize_visible_text(strip_xml_text(paragraph_xml))
        paragraphs.append((match.start(), match.end(), paragraph_text))

    normalized_module = normalize_visible_text(module_heading)
    normalized_next = normalize_visible_text(next_heading)
    module_index = next((index for index, item in enumerate(paragraphs) if normalized_module in item[2]), None)
    next_index = next((index for index, item in enumerate(paragraphs) if normalized_next in item[2]), None)

    if module_index is None:
        raise ValueError(f"Module heading not found: {module_heading}")
    if next_index is None:
        raise ValueError(f"Next heading not found: {next_heading}")
    if module_index >= next_index:
        raise ValueError(f"Invalid module order: {module_heading} -> {next_heading}")

    insert_at = paragraphs[next_index][0]
    return document_xml[:insert_at] + section_xml + document_xml[insert_at:]


def strip_xml_text(xml_fragment: str) -> str:
    return html.unescape(re.sub(r"<[^>]+>", "", xml_fragment))


def normalize_visible_text(text: str) -> str:
    return " ".join(text.split())


def extract_code_block(relative_path: str, start_line: int, end_line: int) -> str:
    path = ROOT / relative_path
    lines = path.read_text(encoding="utf-8").splitlines()
    selected = lines[start_line - 1:end_line]
    return "\n".join(selected)


def build_module_code_xml(
    title: str,
    file_path: str,
    start_line: int,
    end_line: int,
    analysis: list[str],
) -> str:
    code = extract_code_block(file_path, start_line, end_line)
    parts = [
        make_paragraph(title, bold=True, first_line_indent=False, spacing_before=100, spacing_after=60, east_asia_font="KaiTi", size=24),
        make_paragraph(
            f"代码位置：{file_path}:{start_line}-{end_line}",
            first_line_indent=False,
            spacing_after=60,
            ascii_font="Calibri",
            east_asia_font="SimSun",
            size=22,
            color="666666",
        ),
        build_code_block_xml(code),
        make_paragraph("代码分析：", bold=True, first_line_indent=False, spacing_after=40, east_asia_font="KaiTi", size=24),
    ]
    parts.extend(make_paragraph(item, spacing_after=60) for item in analysis)
    return "".join(parts)


def build_module_insertions() -> list[tuple[str, str, str]]:
    return [
        (
            "2.2.1 用户认证模块",
            "2.2.2 用户管理模块",
            build_module_code_xml(
                "核心代码（注册与角色初始化）",
                "backend/src/main/java/com/example/ai/service/impl/AuthServiceImpl.java",
                59,
                95,
                [
                    "这段代码先确定目标角色，再分别处理教师注册密钥校验和管理员禁止自注册规则，使注册入口在业务层就完成角色分流。",
                    "用户基础账号写入后，系统立即补建学生档案或教师档案，保证“身份账号”和“业务档案”同步生成，避免出现只有登录账号却没有业务实体的数据不一致。",
                    "教师注册密钥在用户保存成功后才被标记为已使用，能够把“校验密钥”“创建账号”“补建教师档案”三个动作纳入同一业务流程。",
                ],
            ),
        ),
        (
            "2.2.7 AI 对话模块",
            "2.2.8 RAG 知识库模块",
            build_module_code_xml(
                "核心代码（多模式消息构造与知识库提示拼装）",
                "backend/src/main/java/com/example/ai/service/impl/ChatServiceImpl.java",
                222,
                247,
                [
                    "buildMessagesByMode 通过 switch 分发聊天模式，把直接对话、知识库问答和联网搜索统一收敛到同一消息构造入口，降低了控制器层复杂度。",
                    "知识库模式下，系统先调用 documentService.doSearch 检索相关片段，再把检索结果与用户问题填充到 RAG 模板中，从而把“检索”和“生成”两步明确衔接起来。",
                    "最终返回的是标准 Message 列表而非字符串拼接结果，这使后续的上下文合并、流式输出和多模态扩展都能沿用同一套消息模型。",
                ],
            ),
        ),
        (
            "2.2.8 RAG 知识库模块",
            "2.2.9 OCR 文档解析模块",
            build_module_code_xml(
                "核心代码（向量检索、关键词检索与去重合并）",
                "backend/src/main/java/com/example/ai/service/impl/DocumentServiceImpl.java",
                82,
                102,
                [
                    "doSearch 首先构造带可见性过滤条件的向量检索请求，确保用户只能访问公共知识或本人私有知识，权限边界在检索层就得到控制。",
                    "系统并未只依赖单一路径，而是把向量召回、关键词命中和向量结果重排组合起来，既保留语义检索能力，也兼顾课程名、文件名等精确关键词场景。",
                    "appendDistinct 负责按去重键合并结果并裁剪最终条数，避免同一文档片段重复进入 Prompt，减轻大模型上下文冗余。",
                ],
            ),
        ),
        (
            "2.2.9 OCR 文档解析模块",
            "2.2.10 文件上传模块",
            build_module_code_xml(
                "核心代码（按文件类型分流解析）",
                "backend/src/main/java/com/example/ai/service/impl/RagParseServiceImpl.java",
                57,
                80,
                [
                    "parse 方法先判断文件属于 PDF、图片还是普通结构化文档，再分别进入不同解析分支，体现了文档解析模块的统一入口与策略分发设计。",
                    "对于 PDF 和图片，系统优先走 OCR 分支；对于 DOC、DOCX 等可直接提取正文的文档，则保留 structuredText 与 ocrText 两类结果，增强入库文本完整性。",
                    "解析结果最终封装为 RagParsePreview 返回前端，用户可以在正式入库前先预览、修订抽取文本，从而降低错误 OCR 直接进入知识库的风险。",
                ],
            ),
        ),
        (
            "2.2.11 AI 数据查询模块",
            "2.2.12 会话历史与日志模块",
            build_module_code_xml(
                "核心代码（SQL 结构校验与安全重写）",
                "backend/src/main/java/com/example/ai/service/impl/AiSqlQueryServiceImpl.java",
                212,
                240,
                [
                    "validateAndRewrite 先利用 JSQLParser 把模型生成的 SQL 解析为语法树，并强制限定语句类型只能是简单 SELECT，从结构上阻断更新、删除和子查询等高风险语句。",
                    "在通过语法树后，系统继续校验 FROM、JOIN、SELECT、WHERE、GROUP BY 和 ORDER BY 等各部分字段是否合法，确保模型只能访问当前角色被允许的数据源和字段。",
                    "enforceLimit 会在最终执行前补充结果集上限，既控制查询成本，也防止一次自然语言查询返回过多记录影响系统可用性。",
                ],
            ),
        ),
    ]


def update_docx(input_path: Path, output_path: Path) -> Path:
    document_xml_name = "word/document.xml"
    with zipfile.ZipFile(input_path, "r") as source_docx:
        document_xml = source_docx.read(document_xml_name).decode("utf-8")
        updated_xml = document_xml
        for module_heading, next_heading, section_xml in build_module_insertions():
            updated_xml = insert_section_into_module(updated_xml, module_heading, next_heading, section_xml)

        updated_document_xml = updated_xml.encode("utf-8")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        write_path = output_path
        if output_path.resolve() == input_path.resolve():
            write_path = output_path.with_name(f"{output_path.stem}.tmp{output_path.suffix}")

        with zipfile.ZipFile(write_path, "w") as target_docx:
            for item in source_docx.infolist():
                data = updated_document_xml if item.filename == document_xml_name else source_docx.read(item.filename)
                target_docx.writestr(item, data)

    if write_path != output_path:
        write_path.replace(output_path)
    return output_path


def main() -> None:
    input_path = ROOT / "毕设文档" / "校园知识问答助手系统-总体设计与详细设计-v2.docx"
    output_path = input_path
    actual_path = update_docx(input_path, output_path)
    print(actual_path)


if __name__ == "__main__":
    main()

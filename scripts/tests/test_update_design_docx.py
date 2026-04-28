from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "update_design_docx.py"
SPEC = importlib.util.spec_from_file_location("update_design_docx", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Failed to load module from {MODULE_PATH}")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class UpdateDesignDocxTest(unittest.TestCase):
    def test_insert_section_before_target_heading(self) -> None:
        document_xml = (
            '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
            "<w:body>"
            "<w:p><w:r><w:t>2.2 功能模块设计与实现</w:t></w:r></w:p>"
            "<w:p><w:r><w:t>existing body</w:t></w:r></w:p>"
            "<w:p><w:r><w:t>2.3 安全总体设计</w:t></w:r></w:p>"
            "<w:sectPr/>"
            "</w:body>"
            "</w:document>"
        )

        updated = MODULE.insert_section_before_heading(
            document_xml,
            "2.3 安全总体设计",
            "<w:p><w:r><w:t>2.2.13 核心代码设计</w:t></w:r></w:p>",
        )

        self.assertIn("2.2.13 核心代码设计", updated)
        self.assertLess(updated.index("2.2.13 核心代码设计"), updated.index("2.3 安全总体设计"))
        self.assertTrue(updated.index("existing body") < updated.index("2.2.13 核心代码设计"))

    def test_build_code_block_uses_monospace_paragraphs(self) -> None:
        block_xml = MODULE.build_code_block_xml("line one\n  line two")

        self.assertIn("Courier New", block_xml)
        self.assertIn("w:shd", block_xml)
        self.assertIn("line one", block_xml)
        self.assertIn("line two", block_xml)

    def test_insert_section_into_target_module_before_next_heading(self) -> None:
        document_xml = (
            '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
            "<w:body>"
            "<w:p><w:r><w:t>2.2.1 用户认证模块</w:t></w:r></w:p>"
            "<w:p><w:r><w:t>auth body</w:t></w:r></w:p>"
            "<w:p><w:r><w:t>2.2.2 用户管理模块</w:t></w:r></w:p>"
            "<w:p><w:r><w:t>user body</w:t></w:r></w:p>"
            "<w:sectPr/>"
            "</w:body>"
            "</w:document>"
        )

        updated = MODULE.insert_section_into_module(
            document_xml,
            "2.2.1 用户认证模块",
            "2.2.2 用户管理模块",
            "<w:p><w:r><w:t>核心代码：</w:t></w:r></w:p>",
        )

        self.assertTrue(updated.index("auth body") < updated.index("核心代码："))
        self.assertTrue(updated.index("核心代码：") < updated.index("2.2.2 用户管理模块"))


if __name__ == "__main__":
    unittest.main()

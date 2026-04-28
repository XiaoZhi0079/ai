import importlib.util
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("1.py")
spec = importlib.util.spec_from_file_location("ocr_script", MODULE_PATH)
ocr_script = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ocr_script)


class OcrProcessingTests(unittest.TestCase):
    def test_merge_markdown_documents_preserves_page_order(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            output_dir = Path(tmp_dir)
            pages = ["# Page 1\n\nalpha", "# Page 2\n\nbeta"]

            merged_path = ocr_script.merge_markdown_documents(pages, output_dir)

            self.assertEqual(merged_path, output_dir / "full.md")
            self.assertTrue(merged_path.exists())
            self.assertEqual(
                merged_path.read_text(encoding="utf-8"),
                "# Page 1\n\nalpha\n\n---\n\n# Page 2\n\nbeta\n",
            )

    def test_export_docx_from_markdown_creates_docx_when_dependency_available(self):
        if not getattr(ocr_script, "DOCX_SUPPORT", False):
            self.skipTest("python-docx not installed")

        with tempfile.TemporaryDirectory() as tmp_dir:
            output_dir = Path(tmp_dir)
            docx_path = ocr_script.export_docx_from_markdown("# Title\n\nParagraph", output_dir)

            self.assertEqual(docx_path, output_dir / "full.docx")
            self.assertTrue(docx_path.exists())
            self.assertGreater(docx_path.stat().st_size, 0)


if __name__ == "__main__":
    unittest.main()

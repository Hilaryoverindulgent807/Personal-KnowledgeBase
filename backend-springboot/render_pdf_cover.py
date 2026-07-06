import fitz
import sys
import os

def render_cover(pdf_path, output_path):
    try:
        doc = fitz.open(pdf_path)
        if len(doc) > 0:
            page = doc.load_page(0)
            pix = page.get_pixmap(dpi=150) # render at 150 DPI for good clarity
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            pix.save(output_path)
            print(f"Success: {output_path}")
        else:
            print("Error: Empty PDF")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 render_pdf_cover.py <pdf_path> <output_path>")
    else:
        render_cover(sys.argv[1], sys.argv[2])
